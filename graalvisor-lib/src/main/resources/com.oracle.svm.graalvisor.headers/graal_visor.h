#ifndef __GRAALVISOR_H
#define __GRAALVISOR_H
#include "graal_isolate.h"
#include <sys/types.h>

typedef struct graal_visor_struct {
  void * (*f_host_receive_string) (graal_isolatethread_t *, char*);

  /* Function used to call JDBC operations. */
  char * (*f_host_execute_db_method) (graal_isolatethread_t *, int, char*);
} graal_visor_t;


#endif
