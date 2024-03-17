
JAVA = java -classpath classes:../jar/epts.jar:/usr/share/java/libbzdev.jar

dtest:
	(cd ..; make)
	mkdir -p classes
	javac -d classes \
	-classpath ../jar/epts.jar:/usr/share/java/libbzdev.jar DTest.java
	$(JAVA) DTest

trans:
	(cd ..; make)
	mkdir -p classes
	javac -d classes \
	-classpath ../jar/epts.jar:/usr/share/java/libbzdev.jar *.java 
	$(JAVA) TTest

trans2:
	(cd ..; make)
	mkdir -p classes
	javac -d classes \
	-classpath ../jar/epts.jar:/usr/share/java/libbzdev.jar *.java 
	$(JAVA) TTest2

trans3:
	(cd ..; make)
	mkdir -p classes
	javac -d classes \
	-classpath ../jar/epts.jar:/usr/share/java/libbzdev.jar *.java 
	$(JAVA) TTest3

otto:
	(cd .. ; make)
	cp ottoman3.epts.sv ottoman3.epts
	java -p ../jar -m org.bzdev.epts ottoman3.epts
