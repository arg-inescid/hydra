#include <stdio.h>
#include <signal.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <pthread.h>
#include "queue.h"
#include <string.h>

#define NUM_PROCESSES 20
#define FIFO_FILE "/tmp/input"
#define DEBUG 0
#define NUM_THREADS 2
#define BUFFER_SIZE 1024

pid_t procIDs[NUM_PROCESSES];
pthread_t zombie_reaper;
pthread_t process_assigner;
Queue myQueue;
pthread_mutex_t queue_mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t queue_not_empty_cond = PTHREAD_COND_INITIALIZER;
pthread_t threads[NUM_THREADS];
int counter = 0;

void executeFunction(char *string1, char *string2)
{
  printf("String 1: %s\n", string1);
  printf("String 2: %s\n", string2);
  /*
  void *handle = dlopen("./libexample.so", RTLD_LAZY);
    if (!handle) {
        fprintf(stderr, "Error: %s\n", dlerror());
        exit(EXIT_FAILURE);
    }

    // Declare a function pointer to hold the symbol address
    void (*func)();

    // Retrieve the symbol address
    func = (void (*)())dlsym(handle, "example_function");
    if (!func) {
        fprintf(stderr, "Error: %s\n", dlerror());
        dlclose(handle);
        exit(EXIT_FAILURE);
    }

    // Call the function
    (*func)();

    // Close the library
    dlclose(handle);
    */
}

void process_setup(const char *fifo_path)
{
  int fd;
  char buffer[BUFFER_SIZE];
struct timespec start, end;

    // Get the start time
  fd = open(fifo_path, O_RDONLY);
  if (fd == -1) {
    perror("open");
    exit(EXIT_FAILURE);
  }

    if (clock_gettime(CLOCK_REALTIME, &start) == -1) {
        perror("clock_gettime");
    }
  ssize_t bytes_read = read(fd, buffer, BUFFER_SIZE);
  if (bytes_read == -1) {
    perror("read");
    exit(EXIT_FAILURE);
  }
  close(fd);
  buffer[bytes_read] = '\0';
  char *token;
  token = strtok(buffer, ",");
  if (token != NULL) {
    char *string1 = strdup(token);
    token = strtok(NULL, ",");
    if (token != NULL) {
      char *string2 = strdup(token);
      executeFunction(string1, string2);
      free(string2);
    }
    free(string1);
  }
    if (clock_gettime(CLOCK_REALTIME, &end) == -1) {
        perror("clock_gettime");
    }

        long long int start_ns = (long long int)start.tv_sec * 1000000000LL + start.tv_nsec;
    long long int end_ns = (long long int)end.tv_sec * 1000000000LL + end.tv_nsec;
    long long int elapsed_ns = end_ns - start_ns;

    printf("Start time in nanoseconds: %lld\n", start_ns);
    printf("End time in nanoseconds: %lld\n", end_ns);
    printf("Elapsed time in nanoseconds: %lld\n", elapsed_ns);

}


void signal_handler(int sig)
{
  printf("Signal Received");
}

void ctrl_c_handler(int signal)
{
  printf("Ctrl+C received. Terminating threads...\n");
  for (int i = 0; i < NUM_THREADS; ++i)
  {
    pthread_cancel(threads[i]);
    pthread_join(threads[i], NULL);
  }
  exit(EXIT_SUCCESS);
}

void create_process_pool()
{
  int i;
  pid_t pid;
  for (i = 0; i < NUM_PROCESSES; ++i)
  {
    pid = fork();
    if (pid == -1)
    {
      perror("Error with fork");
      exit(EXIT_FAILURE);
    }
    else if (pid == 0)
    {
      char fifo_path[50];
      snprintf(fifo_path, sizeof(fifo_path), "/tmp/fifo_%d", getpid());
      if (mkfifo(fifo_path, 0666) == -1) {
        perror("mkfifo");
        exit(EXIT_FAILURE);
      }
      process_setup(fifo_path);

      exit(EXIT_SUCCESS);
    }
    else
    {
      procIDs[i] = pid;
    }
  }
}

void *zombie_handler(void *arg)
{
  while (1)
  {
    pid_t pid;
    int status;
    while ((pid = waitpid(-1, &status, WNOHANG)) > 0)
    {
#if DEBUG
      printf("Reaped zombie process with PID %d\n", pid);
#endif

      char fifo_delete[50];
      snprintf(fifo_delete, sizeof(fifo_delete), "/tmp/fifo_%d", pid);
      if (unlink(fifo_delete) == -1)
      {
        perror("unlink");
        exit(EXIT_FAILURE);
      }

      int i;
      for (i = 0; i < NUM_PROCESSES; ++i)
      {
        if (procIDs[i] == pid)
        {
          break;
        }
      }
      pid_t new_pid = fork();
      if (new_pid == -1)
      {
        perror("Error with fork");
        exit(EXIT_FAILURE);
      }
      else if (new_pid == 0)
      {
#if DEBUG
        printf("Child process with PID %d started\n", getpid());
#endif
        char fifo_path[50];
        snprintf(fifo_path, sizeof(fifo_path), "/tmp/fifo_%d", getpid());
        if (mkfifo(fifo_path, 0666) == -1) {
          perror("mkfifo");
          exit(EXIT_FAILURE);
        }
        process_setup(fifo_path);
        exit(EXIT_SUCCESS);
      }
      else
      {
        procIDs[i] = new_pid;
      }
    }
    sleep(2);
  }
  // No return statement needed, since the return type is void
}

/**
 * @brief This method takes input as the payload that has been received from the
 * named pipes. It then parses the payload and adds it to the queue of items for processing.
 *
 *
 * @param str
 */
void tokenize_string(char *str)
{
  const char outer_delimiters[] = "\n";
  const char inner_delimiters[] = ",";
  char *token;
  char *outer_saveptr = NULL;
  char *inner_saveptr = NULL;
  token = strtok_r(str, outer_delimiters, &outer_saveptr);
  while (token != NULL)
  {
    char *inner_token1 = strtok_r(token, inner_delimiters, &inner_saveptr);
    char *inner_token2 = strtok_r(NULL, inner_delimiters, &inner_saveptr);
    if (inner_token1 != NULL && inner_token2 != NULL)
    {
      pthread_mutex_lock(&queue_mutex);
      enqueue(&myQueue, inner_token1, inner_token2);
      pthread_cond_signal(&queue_not_empty_cond);
      pthread_mutex_unlock(&queue_mutex);
    }
    token = strtok_r(NULL, outer_delimiters, &outer_saveptr);
  }
}

void *process_assignment(void *arg)
{
  while (1)
  {
    pthread_mutex_lock(&queue_mutex);
    while (isEmptyQueue(&myQueue))
    {
      pthread_cond_wait(&queue_not_empty_cond, &queue_mutex);
    }
    Node *task = dequeue(&myQueue);
    pthread_mutex_unlock(&queue_mutex);
#if DEBUG
    printf("Processing task: %s, %s\n", task->string1, task->string2);
#endif
    if (kill(procIDs[counter++ % NUM_PROCESSES], SIGUSR1) == -1)
    {
      perror("Signal failed");
      exit(EXIT_FAILURE);
    }

    char fifo_path[50];
    snprintf(fifo_path, sizeof(fifo_path), "/tmp/fifo_%d", procIDs[counter++ % NUM_PROCESSES]);

    int fd = open(fifo_path, O_WRONLY);
    if (fd != -1)
    {
      char message[100]; // Adjust buffer size as needed
      snprintf(message, sizeof(message), "%s,%s", task->string1, task->string2);
      write(fd, message, strlen(message));
      close(fd);
    }
    free(task);
  }
  return NULL;
}

int assign_task()
{
  int fd;
  char buffer[BUFSIZ];
  while (1)
  {
    fd = open(FIFO_FILE, O_RDONLY);
    if (fd == -1)
    {
      perror("Error opening Named Pipe");
      exit(EXIT_FAILURE);
    }
    read(fd, buffer, BUFSIZ);
    tokenize_string(buffer);
    close(fd);
  }
  return 0;
}

int main()
{

  initQueue(&myQueue);

  if (signal(SIGINT, ctrl_c_handler) == SIG_ERR || signal(SIGUSR1, signal_handler) == SIG_ERR)
  {
    perror("Failed to register signal handler");
    return 1;
  }

  create_process_pool();

  void *(*thread_functions[NUM_THREADS])(void *) = {zombie_handler, process_assignment};
  const char *thread_names[NUM_THREADS] = {"Zombie Handler", "Process Assigner"};

  for (int i = 0; i < NUM_THREADS; ++i)
  {
    if (pthread_create(&threads[i], NULL, thread_functions[i], NULL) != 0)
    {
      perror("pthread_create failed");
      return 1;
    }
  }
  assign_task();

  return 0;
}
