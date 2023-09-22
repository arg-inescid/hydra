#ifndef __GRAALVISOR_H
#define __GRAALVISOR_H
#include "graal_isolate.h"
#include <sys/types.h>

typedef struct graal_visor_struct {
  void * (*f_host_receive_string) (graal_isolatethread_t *, char*);

  /* General DB functions. */
  int    (*f_host_obtain_db_connection) (graal_isolatethread_t *, char*, char*, char*);
  int    (*f_host_execute_db_query)     (graal_isolatethread_t *, int, char*);
  int    (*f_host_return_db_connection) (graal_isolatethread_t *, int);

  /* ResultSet functions. */
  int    (*f_host_resultset_next)            (graal_isolatethread_t *, int);
  int    (*f_host_resultset_getint_index)    (graal_isolatethread_t *, int, int);
  int    (*f_host_resultset_getint_label)    (graal_isolatethread_t *, int, char*);
  char * (*f_host_resultset_getstring_index) (graal_isolatethread_t *, int, int);
  char * (*f_host_resultset_getstring_label) (graal_isolatethread_t *, int, char*);

  /* ResultSetMetaData functions. */
  int    (*f_host_resultsetmetadata_getcolumncount) (graal_isolatethread_t *, int);
  char * (*f_host_resultsetmetadata_getcolumnname)  (graal_isolatethread_t *, int, int);
} graal_visor_t;


#endif
