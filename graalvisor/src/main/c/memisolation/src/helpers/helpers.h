#include "../utils/appmap.h"

void init_app_array(char* array[]);
char* extract_basename(const char* filePath);
void get_memory_regions(AppMap* map, char* id, const char* path);