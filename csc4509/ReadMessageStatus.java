package csc4509;
/*
 * $Id: ReadMessageStatus.java 715 2012-06-07 20:11:56Z cbac $
 */
public enum ReadMessageStatus {
    ReadUnstarted,
    ReadHeaderStarted ,
    ReadHeaderCompleted ,
    ReadDataStarted ,
    ReadDataCompleted ,
    ChannelClosed;
}