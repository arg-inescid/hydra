#ifndef __GRAALVISOR_H
#define __GRAALVISOR_H
#include "graal_isolate.h"
#include <sys/types.h>

typedef struct graal_visor_struct {
  graal_isolate_t* f_host_isolate;
  void * (*f_host_receive_string)(graal_isolatethread_t *, char*);
  int (*f_host_open_file)(graal_isolatethread_t *, char*, int);
  int (*f_host_close_file)(graal_isolatethread_t *, int);
  int (*f_host_write_bytes)(graal_isolatethread_t *, int, char*, size_t);
  int (*f_host_read_bytes)(graal_isolatethread_t *, int, char*, int , int);

  int (*f_host_open_db_connection)(graal_isolatethread_t *, char*, char*, char*);
  int (*f_host_execute_db_query)(graal_isolatethread_t *, int, char*, char*, int);
  int (*f_host_close_db_connection)(graal_isolatethread_t *, int);
} graal_visor_t;


#endif
