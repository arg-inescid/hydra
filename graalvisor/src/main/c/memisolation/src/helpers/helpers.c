#define _GNU_SOURCE
#include <sys/mman.h>
#include <stdio.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include "helpers.h"

/* Auxiliary functions */
void
init_supervisors(struct Supervisor supervisors[], int size)
{
	for (int i = 0; i < size; i++) {
		sem_init(&supervisors[i].sem, 0, 0);
		supervisors[i].status = ACTIVE;
		supervisors[i].execution = MANAGED;
		supervisors[i].fd = 0;
		strcpy(supervisors[i].app, "");
	}
}

void
init_thread_count(int threadCount[], int size)
{
	for (int i = 0; i < size; i++) {
		threadCount[i] = 0;
	}
}

void
init_cache(char* cache[], int size)
{
	for (int i = 0; i < size; i++) {
		cache[i] = strdup("");
	}
}

char*
extract_basename(const char* filePath)
{
	char* baseName = strrchr(filePath, '/');
	if (baseName != NULL) {
		return baseName + 1;
	}
	return (char*)filePath;
}

void
get_memory_regions(AppMap* map, char* id, const char* path)
{
	const char* libraryName = extract_basename(path);

	FILE* mapsFile = fopen("/proc/self/maps", "r");
	if (!mapsFile) {
		fprintf(stderr, "Failed to open /proc/self/maps\n");
		exit(EXIT_FAILURE);
	}

	char line[256];
	MemoryRegion memReg;

	while (fgets(line, sizeof(line), mapsFile)) {

		if (strstr(line, libraryName) == NULL) {
		continue;
		}

        unsigned long start, finish;
        char r, w, x;

        sscanf(line, "%lx-%lx %c%c%c",
            &start, &finish, &r, &w, &x);

        int prot_flags = 0;
        if (r == 'r') prot_flags |= PROT_READ;
        if (w == 'w') prot_flags |= PROT_WRITE;
        if (x == 'x') prot_flags |= PROT_EXEC;

        memReg.address = (void*)start;
        memReg.size = finish - start;
        memReg.flags = prot_flags;

        insert_app(map, id, memReg);
    }
	
    fclose(mapsFile);
}
