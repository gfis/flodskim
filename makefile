#!/usr/bin/make

# @(#) $Id: makefile 13 2008-09-05 05:58:51Z gfis $
# 2016-09-11: usage, regression
# 2013-11-05: Georg Fischer

APPL=flodskim
JAVA=java -jar dist/$(APPL).jar
# JAVA=java -cp dist/$(APPL).jar org.teherba.$(APPL).Main
REGR=$(JAVA) org.teherba.common.RegressionTester
# REGR=java -Xss512m -Xms1024m -Xmx2048m -cp dist/$(APPL).jar org.teherba.common.RegressionTester
DIFF=diff -y --suppress-common-lines --width=160
DIFF=diff -w -rs -C0
SRC=src/main/java/org/teherba/$(APPL)
TOM=c:/var/lib/tomcat/
TOMC=$(TOM)/webapps/$(APPL)
TESTDIR=test
# the following can be overriden outside for single or subset tests,
# for example make regression TEST=U%
TEST="%"
# for Windows, SUDO should be empty
SUDO=

BLOCK=0
FILE=test/bsm100.tett2.dsk

all: regression
usage:
	java -jar dist/$(APPL).jar
all: sandbox
#-------------------------------------------------------------------
# Perform a regression test 
regression: 
	java -cp dist/$(APPL).jar \
			org.teherba.common.RegressionTester $(TESTDIR)/all.tests $(TEST) 2>&1 \
	| tee $(TESTDIR)/regression.log
	grep FAILED $(TESTDIR)/regression.log
#
# Recreate all testcases which failed (i.e. remove xxx.prev.tst)
# Handle with care!
# Failing testcases are turned into "passed" and are manifested by this target!
recreate: recr1 regr2
recr0:
	grep -E '> FAILED' $(TESTDIR)/regression*.log | cut -f 3 -d ' ' | xargs -l -ißß echo rm -v test/ßß.prev.tst
recr1:
	grep -E '> FAILED' $(TESTDIR)/regression*.log | cut -f 3 -d ' ' | xargs -l -ißß rm -v test/ßß.prev.tst
regr2:
	make regression TEST=$(TEST) > x.tmp
regeval:
	grep -iHE "tests (FAILED|passed|recreated)" $(TESTDIR)/*.log
# test whether all defined tests in common.tests have *.prev.tst results and vice versa
check_tests:
	grep -E "^TEST" $(TESTDIR)/all.tests   | cut -b 6-8 | sort | uniq -c > $(TESTDIR)/tests_formal.tmp
	ls -1 $(TESTDIR)/*.prev.tst            | cut -b 6-8 | sort | uniq -c > $(TESTDIR)/tests_actual.tmp
	diff -y --suppress-common-lines --width=32 $(TESTDIR)/tests_formal.tmp $(TESTDIR)/tests_actual.tmp
#---------------------------------------------------
jfind:
	find src -iname "*.java" | xargs -l grep -H $(JF)
rmbak:
	find src -iname "*.bak"  | xargs -l rm -v
#---------------------------------------------------
headers:
	rm -f test/headers.tmp
	ls -1 test/*.dsk | xargs -t -l -i{} $(JAVA) -buffer dsk -read {} -dump 0 680  >> test/headers.tmp
h2:
	for VAR in test/*.dsk; do \
	echo "=============================================================="; \
	echo $$VAR; \
	$(JAVA) -buffer dsk -read $$VAR -dump 0 680;\
	done
dump:
	$(JAVA) -buffer dsk -read $(FILE) -dump 0 40000
block:
	$(JAVA) -buffer dsk -read $(FILE) -system ta-vs -block $(BLOCK)
dir:
	$(JAVA) -buffer dsk -read $(FILE) -system ta-vs -dir

# copy1 copy2 dir1 dir2

run:
	$(JAVA) -buffer base -read test/escu/escu2.dsk -dump 0 400
copy1:
	$(JAVA) -buffer dsk  -read test/escu/escu1.dsk -system dec-rx50 -copy test/escu/disk1
copy2:
	$(JAVA) -buffer dsk  -read test/escu/escu2.dsk -system dec-rx50 -copy test/escu/disk2
dir1:
	$(JAVA) -buffer dsk  -read test/escu/escu1.dsk -system dec-rx50 -dir | tee test/escu/disk1/aa_disk1.dir
dir2:
	$(JAVA) -buffer dsk  -read test/escu/escu2.dsk -system dec-rx50 -dir | tee test/escu/disk2/aa_disk2.dir
dsk1:
	$(JAVA) -buffer dsk  -read test/escu/escu1.dsk -dump 2800 40000
dsk2:
	$(JAVA) -buffer dsk  -read test/escu/escu2.dsk -dump 2800 40000
block1:
	$(JAVA) -buffer dsk  -read test/escu/escu1.dsk -system dec-rx50 -block $(BLOCK)
block2:
	$(JAVA) -buffer dsk  -read test/escu/escu2.dsk -system dec-rx50 -block $(BLOCK)
javadoc:
	ant javadoc
deploy:
	ant deploy
zip:
	rm -f $(APPL).zip
	find . | grep -v "test/" | zip -@ $(APPL).zip
#-----------
tgz:
	tar czvf $(APPL)_`/bin/date +%Y%m%d`.tgz src test etc web *.wsd* *.xml makefile
#-----------------------------------------------------------------------------------
dumpa:
	dump test/escu/escu1.dsk > test/escu/escu1.tmp
	dump test/escu/escu2.dsk > test/escu/escu2.tmp
