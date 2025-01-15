app = "$(shell pwd)/libhello.so"
NC ='\033[0m'
RED = '\033[0;31m'
GREEN = '\033[0;32m'

all:
	gcc-11 -O0 -g -c -fPIC hello.c
	gcc-11 -O0 -g -shared -o libhello.so *.o

run-checkpoint:
	rm -f snapshot.mem
	rm -f snapshot.meta
	$(CURDIR)/../run-env.sh ../main checkpoint $(app) > checkpoint.log 2>&1

run-restore:
	$(CURDIR)/../run-env.sh ../main restore $(app) > restore.log 2>&1

run: all run-checkpoint run-restore

test: run
	@-grep --color=always -e warning -e error checkpoint.log restore.log
	if grep -q "It's contents are: this should be copied" restore.log && grep -q "new buff addr = 0xa00000001500" restore.log ; then printf ${GREEN}OK${NC}\\n; else printf ${RED}NOK${NC} && printf " contents of buffer weren't restored\\n"; fi
	# TODO: read mallinfo to check if old checkpointed pointers are still using memory

clean:
	rm -f *.txt *.o *.so *.snap
