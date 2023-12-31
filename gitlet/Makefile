# This makefile is defined to give you the following targets:
#
#    default: The default target: Compiles $(PROG) and whatever it 
#	   depends on.
#    style: Run our style checker on the project source files.  Requires that
#           the source files compile.
#    check: Compile $(PROG), if needed, and then for each file, F.in, in
#	   directory testing, use F.in as input to "java $(MAIN_CLASS)" and
#          compare the output to the contents of the file names F.out.
#          Report discrepencies.
#    clean: Remove all the .class files produced by java compilation, 
#          all Emacs backup files, and testing output files.
#
# In other words, type 'gmake' to compile everything; 'gmake check' to 
# compile and test everything, and 'gmake clean' to clean things up.
# 
# You can use this file without understanding most of it, of course, but
# I strongly recommend that you try to figure it out, and where you cannot,
# that you ask questions.  The Lab Reader contains documentation.

STYLEPROG = style61b

JFLAGS = -g -Xlint:unchecked -Xlint:deprecation

CLASSDIR = ../classes

DOCS = ../docs

JAVADOC_FLAGS = -private -Xdoclint:none 

# See comment in ../Makefile
PYTHON = python3

RMAKE = "$(MAKE)"

# A CLASSPATH value that (seems) to work on both Windows and Unix systems.
# To Unix, it looks like ..:$(CLASSPATH):JUNK and to Windows like
# JUNK;..;$(CLASSPATH).
CPATH = "..:$(CLASSPATH):;..;$(CLASSPATH)"

# All .java files in this directory.
SRCS := $(wildcard *.java)

.PHONY: default check clean style acceptance unit doc

# As a convenience, you can compile a single Java file X.java in this directory
# with 'make X.class'
%.class: %.java
	javac $(JFLAGS) -cp $(CPATH) $<

# First, and therefore default, target.
default: compile

compile: $(SRCS)
	javac $(JFLAGS) -cp $(CPATH) $(SRCS)

style: default
	$(STYLEPROG) $(SRCS) 

check:
	$(RMAKE) -C .. PYTHON=$(PYTHON) check

check2:
	$(RMAKE) -C .. PYTHON=$(PYTHON) check2

acceptance:
	$(RMAKE) -C .. PYTHON=$(PYTHON) acceptance

unit: default
	java -ea -cp $(CPATH) gitlet.UnitTest

unit2: default
	java -ea -cp $(CPATH) gitlet.UnitTest
	$(RM) *~ *.class sentinel

run: default
	java -ea -cp $(CPATH) gitlet.Main $(GITLET_ARGS)
	$(RM) *~ *.class sentinel

run_u: default
	java  -Duser.dir=/Users/solcheung/repo/proj3/testing_unit -ea -cp $(CPATH) gitlet.Main $(GITLET_ARGS)
	$(RM) *~ *.class sentinel

doc:
	if [ ! -d $(DOCS) ]; then mkdir $(DOCS); fi
	javadoc -d $(DOCS) -link https://docs.oracle.com/javase/10/docs/api \
	        $(JAVADOC_FLAGS) -sourcepath .. gitlet

# 'make clean' will clean up stuff you can reconstruct.
clean:
	$(RM) *~ *.class sentinel


