#
# $Revision: 1.34 $
#

# Java Compiler to use
# E.g: make all JAVAC=jikes
JAVAC = javac

# Location to place install jarfiles and doc
# If relative, it's w.r.t this Makefile 
# (default is parent directory of this Makefile)
# E.g: make install WEKAHOME=/home/ml/java
WEKAHOME = .

.PHONY: all optimized debug clean install archive doc

all : 
	(cd core; make all JAVAC=$(JAVAC))
	(cd classifiers; make all JAVAC=$(JAVAC))
	(cd filters; make all JAVAC=$(JAVAC))
	(cd estimators; make all JAVAC=$(JAVAC))
	(cd associations; make all JAVAC=$(JAVAC))
	(cd clusterers; make all JAVAC=$(JAVAC))
	(cd attributeSelection; make all JAVAC=$(JAVAC))
	(cd datagenerators; make all JAVAC=$(JAVAC))
	(cd experiment; make all JAVAC=$(JAVAC))
	(cd gui; make all JAVAC=$(JAVAC))

optimized : 
	(cd core; make optimized JAVAC=$(JAVAC))
	(cd classifiers; make optimized JAVAC=$(JAVAC))
	(cd filters; make optimized JAVAC=$(JAVAC))
	(cd estimators; make optimized JAVAC=$(JAVAC))
	(cd associations; make optimized JAVAC=$(JAVAC))
	(cd clusterers; make optimized JAVAC=$(JAVAC))
	(cd attributeSelection; make optimized JAVAC=$(JAVAC))
	(cd datagenerators; make optimized JAVAC=$(JAVAC))
	(cd experiment; make optimized JAVAC=$(JAVAC))
	(cd gui; make optimized JAVAC=$(JAVAC))

debug :
	(cd core; make debug JAVAC=$(JAVAC))
	(cd classifiers; make debug JAVAC=$(JAVAC))
	(cd filters; make debug JAVAC=$(JAVAC))
	(cd estimators; make debug JAVAC=$(JAVAC))
	(cd associations; make debug JAVAC=$(JAVAC))
	(cd clusterers; make debug JAVAC=$(JAVAC))
	(cd attributeSelection; make debug JAVAC=$(JAVAC))
	(cd datagenerators; make debug JAVAC=$(JAVAC))
	(cd experiment; make debug JAVAC=$(JAVAC))
	(cd gui; make debug JAVAC=$(JAVAC))

clean : 
	(cd core; make clean)
	(cd classifiers; make clean) 
	(cd filters; make clean)
	(cd estimators; make clean)
	(cd associations; make clean)
	(cd clusterers; make clean)
	(cd attributeSelection; make clean)
	(cd datagenerators; make clean)
	(cd experiment; make clean)
	(cd gui; make clean)

# Creates Javadoc documentation in directory ../doc
doc :
	(cd ..; \
	(mkdir doc 2>&1 ) >/dev/null ; \
	javadoc -J-mx100m -public -author -version -1.1 -d doc \
	weka.core \
	weka.core.converters \
	weka.classifiers \
	weka.classifiers.j48 \
	weka.classifiers.m5 \
	weka.classifiers.kstar \
	weka.classifiers.neural \
	weka.classifiers.evaluation \
	weka.classifiers.adtree \
	weka.filters \
	weka.estimators \
	weka.associations \
	weka.clusterers \
	weka.attributeSelection \
	weka.datagenerators \
	weka.experiment \
	weka.gui \
	weka.gui.experiment \
	weka.gui.explorer \
	weka.gui.visualize \
	weka.gui.treevisualizer \
	weka.gui.streams ; \
	touch doc/index.html; for page in `ls doc/*.html`; \
	do cat $$page | sed "s/Index<\/A>  <A HREF=\"help.html\"/Index<\/a>  <a href=\"http:\/\/www.cs.waikato.ac.nz\/ml\/weka\/index.html\">WEKA\'s home<\/a>  <A HREF=\"help.html\"/g" > $$page.temp; mv $$page.temp $$page; \
	cat $$page | sed "s/Index<\/A><\/PRE/Index<\/a>  <a href=\"http:\/\/www.cs.waikato.ac.nz\/ml\/weka\/index.html\">WEKA\'s home<\/a><\/PRE/g" > $$page.temp; mv $$page.temp $$page; done; \
	sed 's/help.html/..\/Tutorial.pdf/g' \
	< doc/packages.html > packages_temp.html; \
	mv packages_temp.html doc/packages.html)

# Makes source and binary jarfiles and docs into WEKAHOME
install : all
	(cd ..; \
	echo "Main-Class: weka.gui.GUIChooser" > manifest.tmp ;\
	jar cvfm $(WEKAHOME)/weka.jar manifest.tmp \
	weka/core/*.class \
	weka/core/converters/*.class \
	weka/classifiers/*.class \
	weka/classifiers/j48/*.class \
	weka/classifiers/m5/*.class \
	weka/classifiers/kstar/*.class \
	weka/classifiers/neural/*.class \
	weka/classifiers/evaluation/*.class \
	weka/classifiers/adtree/*.class \
	weka/filters/*.class \
	weka/estimators/*class \
	weka/associations/*.class \
	weka/clusterers/*.class \
	weka/attributeSelection/*.class \
	weka/datagenerators/*.class \
	weka/experiment/*.class \
	weka/experiment/*.props \
	weka/experiment/*.policy \
	weka/gui/*.class \
	weka/gui/*.props \
	weka/gui/*.jpg \
	weka/gui/*.gif \
	weka/gui/experiment/*.class \
	weka/gui/explorer/*.class \
	weka/gui/visualize/*.class \
	weka/gui/visualize/*.props \
	weka/gui/treevisualizer/*.class \
	weka/gui/streams/*.class \
	; \
	jar cvf $(WEKAHOME)/weka-src.jar \
	weka/core/*.java \
	weka/core/converters/*.java \
	weka/classifiers/*.java \
	weka/classifiers/j48/*.java \
	weka/classifiers/m5/*.java \
	weka/classifiers/kstar/*.java \
	weka/classifiers/neural/*.java \
	weka/classifiers/evaluation/*.java \
	weka/classifiers/adtree/*.java \
	weka/filters/*.java \
	weka/estimators/*java \
	weka/associations/*.java \
	weka/clusterers/*.java \
	weka/attributeSelection/*.java \
	weka/datagenerators/*.java \
	weka/experiment/*.java \
	weka/experiment/*.props \
	weka/experiment/*.policy \
	weka/gui/*.java \
	weka/gui/*.props \
	weka/gui/*.jpg \
	weka/gui/*.gif \
	weka/gui/experiment/*.java \
	weka/gui/explorer/*.java \
	weka/gui/visualize/*.java \
	weka/gui/visualize/*.props \
	weka/gui/treevisualizer/*.java \
	weka/gui/streams/*.java \
	;\
	rm manifest.tmp ;\
	jar cvf remoteEngine.jar \
	weka/core/Queue*.class \
	weka/experiment/*_*.class \
	weka/experiment/RemoteEngine*.class \
	weka/experiment/Compute.class \
	weka/experiment/Task.class \
	weka/experiment/TaskStatusInfo.class \
	;\
	cp weka/experiment/remote.policy . ; \
	cp weka/experiment/DatabaseUtils.props . ; \
	jar cvf $(WEKAHOME)/remoteExperimentServer.jar \
	remoteEngine.jar \
	remote.policy \
	DatabaseUtils.props \
	;\
	rm remoteEngine.jar remote.policy DatabaseUtils.props ;\
	(mkdir $(WEKAHOME)/doc 2>&1 ) >/dev/null ; \
	javadoc -J-mx100m -public -author -version -1.1 -d $(WEKAHOME)/doc \
	weka.core \
	weka.core.converters \
	weka.classifiers \
	weka.classifiers.j48 \
	weka.classifiers.m5 \
	weka.classifiers.kstar \
	weka.classifiers.neural \
	weka.classifiers.evaluation \
	weka.classifiers.adtree \
	weka.filters \
	weka.estimators \
	weka.associations \
	weka.clusterers \
	weka.attributeSelection \
	weka.datagenerators \
	weka.experiment \
	weka.gui \
	weka.gui.experiment \
	weka.gui.explorer \
	weka.gui.visualize \
	weka.gui.treevisualizer \
	weka.gui.streams; \
	for page in `ls $(WEKAHOME)/doc/*.html`; \
	do cat $$page | sed "s/Index<\/A>  <A HREF=\"help.html\"/Index<\/a>  <a href=\"http:\/\/www.cs.waikato.ac.nz\/ml\/weka\/index.html\">WEKA\'s home<\/a>  <A HREF=\"help.html\"/g" > $$page.temp; mv $$page.temp $$page; \
	cat $$page | sed "s/Index<\/A><\/PRE/Index<\/a>  <a href=\"http:\/\/www.cs.waikato.ac.nz\/ml\/weka\/index.html\">WEKA\'s home<\/a><\/PRE/g" > $$page.temp; mv $$page.temp $$page; done; \
	sed 's/help.html/..\/Tutorial.pdf/g' \
	< $(WEKAHOME)/doc/packages.html > $(WEKAHOME)/packages_temp.html; \
	mv $(WEKAHOME)/packages_temp.html $(WEKAHOME)/doc/packages.html \
	)

# Creates a snapshot of the entire directory structure
archive :
	(cd ..; \
	tar czf archive/weka`date +%d%b%Y`.tar.gz  weka)
