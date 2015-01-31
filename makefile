#!/usr/bin/make

# @(#) $Id: makefile 13 2008-09-05 05:58:51Z gfis $
APPL=flodskim
JAVA=java -cp dist/$(APPL).jar org.teherba.$(APPL).Main
BLOCK=0
FILE=test/bsm100.tett2.dsk

all: block
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
