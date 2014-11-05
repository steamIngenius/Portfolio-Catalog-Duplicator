Portfolio-Catalog-Duplicator
============================

Operation: 
----------

Configure the server information and administrator login below. The program will connect to the
server and present the user with a list of catalogs. Select the origin catalog first and the destination
after. The program then pages through 1000 records at a time and creates a new record in the
destination catalog with the correct path to each asset from the first.

After the records are 'copied' you'll still need to regen thumbs/previews and pull in metadata.
NONE OF THE METADATA FROM THE ORIGIN CATALOG IS COPIED - Anything that is not embedded in the file
will not make the transition.

Compile: javac -classpath dam-client.jar:. Base64.java Duplicate.java
Run: java -classpath dam-client.jar:. Duplicate

SCRIPTS:
--------

Compile and run: ./go
Compile only: ./compile
Run only: ./run

GOAL:
-----

To create a tool that can duplicate the contents of a catalog by copying all the records
to another catalog.

CURRENT FEATURES:
-----------------

[x] Present a list of catalogs for a user to select from.

PLANNED FEATURES:
-----------------

[ ] If the process fails for a record, tell us which one!
[ ] Copy all record metadata
[ ] Copy galleries

NOTE:
-----

This program actually works! It is also very hacky and the code is very ugly.
Hopefully this will change over time as I work on it and learn more about Portfolio's API.
