# Chat multiserveur

Projet r�alis� par Benoit Tellier, et disponible sous license GPL.

## Structure du d�pot :

Notes sur les algorithmes utilis�s, ainsi que r�ponse aux questions de TPs :

	notes/*

Fichier sources de l'application :

	Chat/*

Diagramme de classe ( vous pouvez le consulter [ici](https://www.gliffy.com/go/html5/5855793?app=1b5094b0-6042-11e2-bcfd-0800200c9a66) )

	asset/*

Le pr�sent README :

	README.md

## Fonctionnalit�s

Nous cherchons � r�aliser un Chat distribu� avec plusieurs serveurs. Nous souhaitons, autant que possible, que notre architecture supporte les changements de topologie aux niveau des serveurs ( ajout ou d�part d'un ou plusieurs serveurs ).

Note : Autant que possible indique que les solutions mises en place pour g�rer les changements de topologie ne seront pas parfait, mais au moins pire ( ce qui est un choix judicieux compte tenu du temps impartit...

Un client pourra �tablir les actions suivantes :

  - Se connecter � un serveurs en sp�cifiant un pseudo
  - Envoyer un message � l'ensemble des personnes pr�sentent sur le Chat
  - Obtenir la liste des clients connect�s sur notre r�seau
  - Obtenir la liste des serveurs connect�s
  - Envoyer un message � un, et un seul des clients connect�s ( message priv� )
  - �tablir une connection de spare avec un autre serveur, sur laquelle on basculera en cas de n�c�ssit�.

Nous ferons attention aussi aux points suivants :

  - Quand un client peut dialoguer avec un client avec qui il ne pouvait pas dialoguer avant, il re�oit une notification indiquant qu'il peut dialoguer avec ce client
  - Inversement quand on ne peut plus dialoguer avec un client, on re�oit un message nous l'indicant.

Les points mentionn�s ci dessus surviennent lors des connections / d�connection de client ou de serveurs.

Nous porterons aussi une attention particuli�re � la d�tection et au traitement des erreurs r�seau ( on adopte un politique assez s�v�re... )