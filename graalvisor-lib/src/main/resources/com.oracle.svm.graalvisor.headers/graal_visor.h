#ifndef __GRAALVISOR_H
#define __GRAALVISOR_H
#include "graal_isolate.h"
#include <sys/types.h>

typedef struct graal_visor_struct {
  void * (*f_host_receive_string)      (graal_isolatethread_t *, char*);
  int    (*f_host_obtain_db_connection)(graal_isolatethread_t *, char*, char*, char*);
  int    (*f_host_execute_db_query)    (graal_isolatethread_t *, int, char*, char*, int);
  int    (*f_host_return_db_connection)(graal_isolatethread_t *, int);
} graal_visor_t;


#endif
