/*
 * man_supervisor.c
 *
 */
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <linux/seccomp.h>
#include <stdlib.h>  
// #include <linux/filter.h>
// #include <linux/audit.h>
#include <sys/ioctl.h>
#include "mansupervisor.h"

volatile atomic_int fd_list[MAX_FDS] = {0};
struct pollfd fds[MAX_FDS] = {0};
int null_fd = -1;

void add_fd(int fd) {

    if(fd_list[fd]) return;
    
    int expected_value = 0;
    int desired_value = 1;
    
    // Updating the list to display the fd usage
    if (!atomic_compare_exchange_strong(&fd_list[fd], &expected_value, desired_value)) 
        printf("fd_list[%d] was not %d, operation failed.\n", fd, expected_value);
    
    if (fd < MAX_FDS) {
        fds[fd].fd = fd; // Updating the fd to be polled
        fds[fd].events = POLLIN;
    } else {
        perror("Error: Exceeded maximum number of file descriptors\n");
    }

}

void remove_fd(int fd){

    fprintf(stderr,"Removing fd %d",fd);

    if (fds[fd].fd == null_fd) {
        fprintf(stderr,"fds[%d].fd is already null, operation skipped.\n", fd);
        return;
    }

    int expected_value = 1;
    int desired_value = 0;
    fds[fd].fd = null_fd; // Marking the fd as empty using null fd
    // Updating the list to display the fd usage
    if (!atomic_compare_exchange_strong(&fd_list[fd], &expected_value, desired_value)) {
        fprintf(stderr,"fd_list[%d] was not %d, operation failed.\n", fd, expected_value);
        return;
    }

}

int is_fd_present(int fd) {
    return fd_list[fd]; 
}


int init_null_fd() {
    if (null_fd == -1) {
        // Obtaining dummy null file descriptor
        null_fd = open("/dev/null", O_RDWR);
        if (null_fd == -1) {
            perror("Failed to open /dev/null");
        }
        for(int i = 0; i < MAX_FDS;i++){
            fds[i].fd = null_fd;
        }
    }
    return null_fd;
}