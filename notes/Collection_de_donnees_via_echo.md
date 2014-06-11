# Collection de donn�es via Echo

## Pourquoi cet algorithme

Lors d'un ajout de serveur � notre r�seau, nous prenons le risque de connecter deux r�seaux entre eux. Si tel est le cas, il nous faut un moyen pour retrouver la liste des pseudos des personnes connect�es, ainsi que la liste des serveurs connect�s.

D'o� le besoin de cet algorithme.

## principe

Le concept est qu'on va effectuer un �cho pour collecter des donn�es sur chacun des noeuds.

Chaque noeud chargera dans son message de broadcast ( envoy� � tous sauf � l'�ventuel parent ) les informations qu'on lui demande de transmettre. 
Chaque noeud �coute les donn�es des noeuds voisins et les accumule pour cet �cho. 
Enfin, d�s que chacun des noeuds lui a r�pondu ( il a re�u autant de messages qu'il y a de noeuds auquel il est reli� ), 
il renvoie l'ensemble des donn�es r�colt�es ( en faisant attention � d'imancables doublons ) � son parent si il n'est pas initiateur, 
sinon si il est initiateur, il a collect� l'ensemble des donn�es des noeuds du r�seau.

## Algorithme

	Init p = #p, seq = 0, rcvMsg[p][seq] = 0, waveData[p][seq] = [], father = null
	
	if initiateur
		seq ++
		set message p and seq
		broadcast message to all neightbours
	fsi
	tant que rcvMsg[p][seq] < #neightbours
		receive message from q
		Merge message.data and waveData[p][seq]
		if first time we spot this echo
			father = q
			repleice message.data by our node datas
			broadcast message to all neightbours except q
		rcvMsg[p][seq]++
	done
	if is Initiator
		return waveData[p][seq]
	else
		message.data = waveData[p][seq]
		send message to father
	fsi

## Exemple

Nous avons noeuds :

  - 1 : reli� � 2 et 4   donn�e : w waveData : []
  - 2 : reli� � 1 4 et 3 donn�e : x waveData : []
  - 3 : reli� � 2 et 4   donn�e : y waveData : []
  - 4 : reli� � 1 2 et 4 donn�e : z waveData : []

1 initie l'algorithme :

 - envoie � 2 un paquet avec comme donn�e w
 - envoie � 4 un paquet avec comme donn�e w

2 re�oit un message de 1 avec comme donn�e w

 - waveData : [w]
 - father : 1
 - 2 envoie � 4 un paquet avec comme donn�e x
 - 2 envoie � 3 un paquet avec comme donn�e x

4 re�oit un message de 1 avec comme donn�e w

 - waveData : [w]
 - father : 1
 - 4 envoie un paquet � 2 et 3 avec comme donn�e z


4 re�oit un message de 2 avec comme donn�e x

 - waveData : [w,x]

3 re�oit un paquet de 2 avec comme donn�e x

 - waveData : [x]
 - father : 2
 - 3 envoie � 4 un paquet avec comme donn�e y

2 re�oit un paquet de 4 avec comme donn�e z

 - waveData : [w,x,z]

3 re�oit un paquet de 4 avec comme donn�e z

 - waveData : [x,z]
 - 3 renvoie un paquet � 2 avec comme donn�e [x,y,z]

4 re�oit un paquet de 3 avec comme donn�e y

 - waveData : [w,x,y]
 - 4 renvoie [w,x,y,z] � 1

2 re�oit un message de 3 avec comme donn�e [x,y,z]

 - waveData : [w,x,y,z]
 - 2 renvoie [w,x,y,z] � 1


1 re�oit [w,x,y,z] en provenance de 2 et 4

1 a maintenant connaissance des donn�es du r�seau. Il peut maintenant les diffuser aux autres noeuds.

## Complexit� :

En O( N� ) o� N est le nombre de serveurs.

## Preuve :

On a trivialement ( gr�ce au broadcast ) que chaque serveur re�oit le message. Il renverra donc l'information qu'il porte � son parent, qui le transmettra alors � son parent, et ainsi de suite.
Le noeud initiateur re�oit donc bien les donn�es de tous les noeuds, ce qui est le but souhait�.

## Notes :

J'ai �t� assez tent� de me passer de la diffusion finale en ajoutant la chose suivante :

Quand on broadcast on envoie non plus l'information du noeud mais waveData.

Cet algorithme ne fonctionne pas ( j'ai trouv� un contre exemple ) : 

![contre_exemple](contre_exemple_echo_full.png)

Les liens correspondent aux traits. Les lien plein correspondent aux relations p�re / fils, 
les traits en pointill�s correspondent � des serveurs simplement li�s.

En �x�cutant ce qui est d�crit ci-dessus, on montre que 4 n'aura pas les donn�es de 8, et ainsi de suite