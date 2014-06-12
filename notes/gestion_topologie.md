# Gestion du changement de topologie

Nous cherchons � avoir trois propri�t�s vraies ci possible � tout instant :

 1. Il y a exactement un serveur �lu par r�seau comportant plus d'un serveur
 2. Chaque serveur a connaissance de l'ensemble des clients connect�s sur son r�seau
 3. Chaque serveur a connaissance de l'ensemble des autres serveurs pr�sent sur son r�seau

Ce trois propri�t�s sont mises � mal par les changements de topologie ( ajout ou retrait d'un ou plusieurs serveurs ).

Par exemple :

 - Un ajout de serveur peut entrainer deux r�seaux � �tre reli�, et ainsi avoir deux serveurs �lus.
 - Un ajout de serveur peut entrainer l'ajout de nouveau client / serveurs sur notre r�seau.
 - Un retrait de serveur peut entrainer la coupure de notre r�seau en 2 r�seaux. Ainsi un de ces deux r�seau se retrouvera sans serveur �lu.
 - Un retrait de serveur peut entrainer des clients serveurs � qui nous ne pouvons pas parler.
 - Perte de messages de diffusion FIFO ou causale.

Notre but est de revenir au plus vite � la normale...

## Approche

Pour solutionner ce probl�me, voici la solution que j'ai choisi de mettre en place :

 - En cas de changement de topologie, on lance une �lection, et on interdit les diffusions FIFO et causales ( messages stock�s pour un envoi une fois les propri�t�s topologiques r�tablies ).
 - En fin d'�lection, il appartient au serveur �lu de resynchroniser les clients pr�sent ainsi que la liste des serveurs. Pour se faire :
	- Il lance deux echos afn de collecter les donn�es (liste des serveurs et liste des pseudos). Les d�tails de l'algorithme sont disponibles [ici](Collection_de_donnees_via_echo.md)
	- Une fois qu'il a re�u une de ces listes, il lance ensuite un R broadcast pour la communiquer aux noeuds du r�seau. Idem � la r�ception de la deuxi�me liste.
	- Enfin, chaque noeud qui re�oit cette liste peut � nouveau envoyer des broadcast FIFO et causaux. Il prendront par ailleurs le soin de vider les messages qui attendent.

## Probl�mes li�s � cette approche

Nous bloquons notre �mission de messages pendant le processus d'�lection, et il y a une perte possible de messages en cas de d�connection d'un serveur.

Par ailleur j'ai bien conscience que **c'est une grosse rustine** ... Le bas blesse pour les op�rations effectu�es pendant ce m�chanisme, 
qui sont potentiellement perdu, ou diffus� dans le mauvais ordre, etc... J'ai bien conscience de ne pas avoir donn� une solution au 
probl�me que je me suis pos�. 
Je me rend bien compte que ce probl�me n'a probablement pas de solutions. En revanche, je ne me voyais pas ne pas impl�menter un 
m�chanisme pour essayer de faire quelque chose contre ce probl�me. C'est tout de m�me mieux que de ne rien faire du tout.

