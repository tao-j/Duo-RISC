default: c
executables := $(notdir $(basename $(wildcard src/main/scala/*.scala)))
c:
	sbt "run --backend c --compile --genHarness --test --debug --vcd --debugMem --vcdMem"
# autodetect file changes and rerun
cc:
	sbt "~run --backend c --compile --genHarness --test --debug --vcd --debugMem --vcdMem"
v:
	sbt "run --backend v --genHarness"

# autodetect file changes and rerun
vv:
	sbt "~run --backend v --genHarness"

clean:
	rm -f $(executables)
	rm -rf target*
	rm -rf project/target*
	rm -f *.cpp *.h *.o

clean_all:
	rm -f $(executables)
	rm -rf target*
	rm -rf project/target*
	rm -f *.cpp *.h *.o
	rm -f *.vcd *.v	

.PHONY: all c cc v vv clean clean_all
