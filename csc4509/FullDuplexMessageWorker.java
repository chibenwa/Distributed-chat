package csc4509;

        import java.io.ByteArrayInputStream;
        import java.io.ByteArrayOutputStream;
        import java.io.IOException;
        import java.io.ObjectInputStream;
        import java.io.ObjectOutputStream;
        import java.io.Serializable;
        import java.nio.ByteBuffer;
        import java.nio.channels.SocketChannel;
/*
 * $Id: FullDuplexMsgWorker.java 937 2013-06-04 08:48:52Z conan $
 */
public class FullDuplexMessageWorker {
    /**
     * boolean allowing to add some debug printing
     */
    private boolean debug = false;
    /**
     * this arrays can contain message headers in the first buffer (fixed size)
     * and message body which size is described in the header
     * We need two byte buffers due to asynchronism in input and
     * output. put and get operations are not done at once.
     */
    private ByteBuffer [] inBuffers, outBuffers;
    /**
     * read message status, to describe completeness of data reception
     */
    private ReadMessageStatus readState;
    private SocketChannel rwChan = null;
    private int messType, messSize;
    /**
     * public ctr for an open channel i.e. after accept
     * @param clientChan the socketChannel that has been accepted on server
     */
    public FullDuplexMessageWorker(SocketChannel clientChan) {
        inBuffers = new ByteBuffer [2];
        outBuffers = new ByteBuffer [2];
        inBuffers[0] = ByteBuffer.allocate(Integer.SIZE*2 / Byte.SIZE);
        outBuffers[0] = ByteBuffer.allocate(Integer.SIZE*2 / Byte.SIZE);
        inBuffers[1] = null;
        outBuffers[1] = null;
        readState = ReadMessageStatus.ReadUnstarted; // not yet started to read
        rwChan = clientChan;
    }

    /**
     * To configure the channel in non blocking mode
     */
    public void configureNonBlocking() throws IOException {
        rwChan.configureBlocking(false);
    }
    /**
     * get the current channel of this worker
     * @return my channel
     */
    public SocketChannel getChannel(){
        return rwChan;
    }
    /**
     * send a message using channel
     * @param type message type
     * @param s the message content is a String
     * @return size of the data send
     * @throws IOException
     */
    public long sendMsg(int type, Serializable s) throws IOException{
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        int size;
        ObjectOutputStream oo = new ObjectOutputStream(bo);
        oo.writeObject(s);
        oo.close();
        size = bo.size();
        setHeader(type,size);

        outBuffers[1] = ByteBuffer.allocate(size);
        outBuffers[1].put(bo.toByteArray());
        bo.close();
        outBuffers[1].flip();
        sendBuffers();
        return size;
    }
    /**
     * send the buffers through the connection
     */
    public void sendBuffers() throws IOException{
        rwChan.write(outBuffers);
    }
    /**
     * initialize header with size and type
     */
    public void setHeader(int type, int size){
        messType = type;
        messSize = size;
        outBuffers[0].clear();
        outBuffers[0].putInt(messType);
        outBuffers[0].putInt(size);
        outBuffers[0].flip();
    }
    public void setDataByteBuffer(ByteBuffer outdata){
        outBuffers[1] = outdata;
    }
    /**
     * close the channel
     * @throws IOException
     */
    public void close() throws IOException{
        rwChan.close();
    }
    /**
     * reads a message
     * it considers that
     * @return a ReadMessageStatus to specify read progress
     * @throws IOException
     */
    public ReadMessageStatus readMessage() {
        int recvSize;

        if(readState == ReadMessageStatus.ReadUnstarted) {
            inBuffers[0].clear();
            readState = ReadMessageStatus.ReadHeaderStarted;
        }
        if(readState == ReadMessageStatus.ReadDataCompleted){
            inBuffers[0].clear();
            inBuffers[1] = null;
            readState = ReadMessageStatus.ReadHeaderStarted;
        }
        if(readState == ReadMessageStatus.ReadHeaderStarted){
            if(inBuffers[0].position() < inBuffers[0].capacity()-1){
                try {
                    recvSize = rwChan.read(inBuffers[0]);
                    if (debug) {
                        System.out.println("	Received       : " + recvSize);
                    }
                    if(recvSize == 0){
                        return readState;
                    }
                    if(recvSize < 0 ){
                        readState = ReadMessageStatus.ChannelClosed;
                        close();
                        return readState;
                    }
                    if(inBuffers[0].position() < inBuffers[0].capacity()-1){
                        return readState;
                    }
                } catch(IOException ie){
                    readState = ReadMessageStatus.ChannelClosed;
                    try {
                        close();
                    }
                    catch( IOException closeException){

                    }
                    return readState;

                }
            }
            inBuffers[0].flip();
            if (debug) {
                System.out.println("Position and limit : " + inBuffers[0].position() +" "+ inBuffers[0].limit());
            }
            messType = inBuffers[0].getInt();
            messSize = inBuffers[0].getInt();
            if (debug) {
                System.out.println("Message type and size : " + messType +" "+ messSize);
            }
            inBuffers[0].rewind();
            readState = ReadMessageStatus.ReadHeaderCompleted;
        }
        if(readState == ReadMessageStatus.ReadHeaderCompleted){
            if(inBuffers[1] == null || inBuffers[1].capacity() != messSize){
                inBuffers[1] = ByteBuffer.allocate(messSize);
            }
            readState = ReadMessageStatus.ReadDataStarted;
        }
        if(readState == ReadMessageStatus.ReadDataStarted){
            if(inBuffers[1].position() < inBuffers[1].capacity()-1) {
                try{
                    recvSize = 	rwChan.read(inBuffers[1]);

                    if (debug) {
                        System.out.println("	Received       : " + recvSize);
                    }
                    if(recvSize == 0){
                        return readState;
                    }
                    if(recvSize < 0){
                        close();
                        readState = ReadMessageStatus.ChannelClosed;
                        return readState;
                    }
                } catch(IOException ie){
                    readState = ReadMessageStatus.ChannelClosed;
                    try {
                        close();
                    }
                    catch( IOException closeException){
                    }
                    return readState;
                }
            }
            if (debug) {
                System.out.println("Position and capacity : " + inBuffers[1].position() +" "+ inBuffers[1].capacity());
            }
            if(inBuffers[1].position() >= inBuffers[1].capacity()-1){
                readState = ReadMessageStatus.ReadDataCompleted;
            }
        }
        return readState;
    }
    /**
     * return the Serializable data build out of the data part of the received message when the
     * readStat is ReadDataCompleted.
     * This operation should be stateless for the ByteBuffers, meaning that we can getData and
     * after write the ByteBuffer if necessary
     * @return unserialized data
     */
    public Serializable getData() throws IOException {
        Serializable res = null;
        if(readState == ReadMessageStatus.ReadDataCompleted){
            try {
                inBuffers[1].flip();
                ByteArrayInputStream bi = new ByteArrayInputStream(inBuffers[1].array());
                ObjectInputStream oi = new ObjectInputStream(bi);
                res = (Serializable) oi.readObject();
                oi.close();
                bi.close();
            } catch(ClassNotFoundException ce){
                ce.printStackTrace();
            }
        }
        if( inBuffers == null || inBuffers[1] == null) {
            return null;
        }
        inBuffers[1].rewind();
        return res;
    }
    /**
     * get the message type
     * @return value of message type
     */
    public int getMessType() {
        return messType;
    }
    /**
     * get the message size if the status is at least ReadHeaderCompleted
     * @return message size
     */
    public int getMessSize() {
        return messSize;
    }
    /**
     * get direct access to byteBuffers
     * @return the ByteBuffers
     */
    public ByteBuffer [] getByteBuffers() {
        return inBuffers;
    }
    /**
     * passThroughOutBuffers is used to send prepared byteBuffers through a channel
     * @param setBbs ByteBuffers to send
     * @throws IOException
     */
    public void passThroughOutBuffers(ByteBuffer [] setBbs) throws IOException {
        outBuffers[0].clear();
        setBbs[0].rewind();
        do {
            outBuffers[0].put(setBbs[0].get());
        }while(setBbs[0].position()<setBbs[0].capacity());
        outBuffers[1] = setBbs[1];
        outBuffers[0].flip();
        outBuffers[1].rewind();
        rwChan.write(outBuffers);
    }

}
