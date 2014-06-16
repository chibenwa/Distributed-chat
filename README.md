# Chat multiserveur

Projet réalisé par Benoit Tellier, et disponible sous license GPL.

## Structure du dépot :

Notes sur les algorithmes utilisés, ainsi que réponse aux questions de TPs :

	notes/*

Fichier sources de l'application :

	Chat/*

Diagramme de classe ( vous pouvez le consulter [ici](https://www.gliffy.com/go/html5/5855793?app=1b5094b0-6042-11e2-bcfd-0800200c9a66) )

	asset/*

Le présent README :

	README.md

## Fonctionnalités

Nous cherchons à réaliser un Chat distribué avec plusieurs serveurs. Nous souhaitons, autant que possible, que notre architecture supporte les changements de topologie aux niveau des serveurs ( ajout ou d�part d'un ou plusieurs serveurs ).

Note : Autant que possible indique que les solutions mises en place pour gérer les changements de topologie ne seront pas parfait, mais au moins pire ( ce qui est un choix judicieux compte tenu du temps impartit...

Un client pourra établir les actions suivantes :

  - Se connecter à un serveurs en spécifiant un pseudo
  - Envoyer un message à l'ensemble des personnes présentent sur le Chat
  - Obtenir la liste des clients connectés sur notre réseau
  - Obtenir la liste des serveurs connectés
  - Envoyer un message à un, et un seul des clients connectés ( message privé )
  - établir une connection de spare avec un autre serveur, sur laquelle on basculera en cas de nécéssité.

Nous ferons attention aussi aux points suivants :

  - Quand un client peut dialoguer avec un client avec qui il ne pouvait pas dialoguer avant, il reçoit une notification indiquant qu'il peut dialoguer avec ce client
  - Inversement quand on ne peut plus dialoguer avec un client, on reçoit un message nous l'indicant.

Les points mentionnés ci dessus surviennent lors des connections / déconnection de client ou de serveurs.

Nous porterons aussi une attention particulière à la détection et au traitement des erreurs réseau ( on adopte un politique assez sévère... )