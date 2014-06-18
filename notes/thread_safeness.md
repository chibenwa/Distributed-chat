# Notes sur la gestion du multithreading dans notre application

Nous utilisons deux threads sur notre serveur. Un de ces threads consiste à attendre la saisie d'un utilisateur, l'autre consiste à écouter le réseau et réagir aux demandes qui sont faites.

Concernant le thread "clavier", il est à noté qu'il y a deux types de commandes :

 - Celles qui sont thread safe. Il s'agit :
   - D'établir une connection avec un nouveau serveur
   - De consulter, réserver, ou relacher une ressource
   - Demander la terminaison de l'application distribuée en toute sécurité
 - Les autres commandes ne sont pas thread safe, et ne sont présentes que pour des raisons de debug. Elles disparaitraient dans un environnement de production.

Afin de garantir l'aspect thread safe du premier type de méthodes, différents mécanismes ont été mis en place :

 - Pour les méthodes qui ne demandent qu'un envoie de message, nous garantissons que l'usage des méthodes d'envoie de messages du NetManager permet l'envoi de plusieurs messages en simultané ( un verrou gère les écritures sur chaque channel ). La seule méthode faisant appel à ce mécanisme est la demande de connection à un autre serveur.
 - Pour les méthodes utilisant des métodes de State, il a été préféré de mettre en place une politique de Scheduling. La raison de ce choix est la simplification de la gestion du multithreading en utilisant ce paradigme. Notre selecteur ne reste donc maintenant que temporairement en attente d'évenements, puis ensuite il consulte, et le cas échéant éxécute les action schedulées.

Coté client, nous avons juste à assurer le coté thread safe de l'envoie de messages.