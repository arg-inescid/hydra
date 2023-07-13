<!-- time -->

### Time

```c
adjtimex(struct timex *buf)
clock_adjtime(clockid_t clk_id, struct timex *buf)
clock_adjtime64(clockid_t clk_id, struct timex *buf)
clock_getres(clockid_t clockid, struct timespec *_Nullable res)
clock_getres_time64(clockid_t clockid, struct timespec *_Nullable res)
clock_gettime(clockid_t clockid, struct timespec *tp)
clock_gettime64(clockid_t clockid, struct timespec *tp)
clock_nanosleep(clockid_t clockid, int flags, const struct timespec *request, struct timespec *_Nullable remain)
clock_nanosleep_time64(clockid_t clockid, int flags, const struct timespec *request, struct timespec *_Nullable remain)
getitimer(int which, struct itimerval *curr_value)
gettimeofday(struct timeval *restrict tv, struct timezone *_Nullable restrict tz)
nanosleep(const struct timespec *req, struct timespec *_Nullable rem)
setitimer(int which, const struct itimerval *restrict new_value, struct itimerval *_Nullable restrict old_value)
time(time_t *_Nullable tloc)
timer_create(clockid_t clockid, struct sigevent *_Nullable restrict sevp, timer_t *restrict timerid)
timer_delete(timer_t timerid)
timer_getoverrun(timer_t timerid)
timer_gettime(timer_t timerid, struct itimerspec *curr_value)
timer_gettime64(timer_t timerid, struct itimerspec *curr_value)
timer_settime(timer_t timerid, int flags, const struct itimerspec *restrict new_value, struct itimerspec *_Nullable restrict old_value)
timer_settime64(timer_t timerid, int flags, const struct itimerspec *restrict new_value, struct itimerspec *_Nullable restrict old_value)
timerfd_create(int clockid, int flags)
timerfd_gettime(int fd, struct itimerspec *curr_value)
timerfd_gettime64(int fd, struct itimerspec *curr_value)
timerfd_settime(int fd, int flags, const struct itimerspec *new_value, struct itimerspec *_Nullable old_value)
timerfd_settime64(int fd, int flags, const struct itimerspec *new_value, struct itimerspec *_Nullable old_value)
times(struct tms *buf)
```

### Network
<!-- network -->

```c
accept(int sockfd, struct sockaddr *_Nullable restrict addr, socklen_t *_Nullable restrict addrlen)
accept4(int sockfd, struct sockaddr *_Nullable restrict addr, socklen_t *_Nullable restrict addrlen, int flags)
bind(int sockfd, const struct sockaddr *addr, socklen_t addrlen)
connect(int sockfd, const struct sockaddr *addr, socklen_t addrlen)
getpeername(int sockfd, struct sockaddr *restrict addr, socklen_t *restrict addrlen)
getsockname(int sockfd, struct sockaddr *restrict addr, socklen_t *restrict addrlen)
getsockopt(int sockfd, int level, int optname, void optval[restrict *.optlen], socklen_t *restrict optlen)
listen(int sockfd, int backlog)
recv(int sockfd, void buf[.len], size_t len, int flags)
recvfrom(int sockfd, void buf[restrict .len], size_t len, int flags, struct sockaddr *_Nullable restrict src_addr, socklen_t *_Nullable restrict addrlen)
recvmmsg(int sockfd, struct mmsghdr *msgvec, unsigned int vlen, int flags, struct timespec *timeout)
recvmmsg_time64(int sockfd, struct mmsghdr *msgvec, unsigned int vlen, int flags, struct timespec *timeout)
recvmsg(int sockfd, struct msghdr *msg, int flags)
send(int sockfd, const void buf[.len], size_t len, int flags)
sendmmsg(int sockfd, struct mmsghdr *msgvec, unsigned int vlen, int flags)
sendmsg(int sockfd, const struct msghdr *msg, int flags)
sendto(int sockfd, const void buf[.len], size_t len, int flags, const struct sockaddr *dest_addr, socklen_t addrlen)
setsockopt(int sockfd, int level, int optname, const void optval[.optlen], socklen_t optlen)
shutdown(int sockfd, int how)
socketcall(int call, unsigned long *args)
socketpair(int domain, int type, int protocol, int sv[2])
```

### Signals
<!-- signals -->

```c
alarm(unsigned int seconds)
kill(pid_t pid, int sig)
pause(void)
rt_sigaction(int signum, const struct sigaction *_Nullable restrict act, struct sigaction *_Nullable restrict oldact, size_t sigsetsize)
rt_sigpending(sigset_t *set, size_t sigsetsize)
rt_sigprocmask(int how, const kernel_sigset_t *_Nullable set, kernel_sigset_t *_Nullable oldset, size_t sigsetsize)
rt_sigqueueinfo(pid_t tgid, int sig, siginfo_t *info)
rt_sigreturn()
rt_sigsuspend(const sigset_t *mask, size_t sigsetsize)
rt_sigtimedwait(const sigset_t *restrict set, siginfo_t *restrict info, const struct timespec *restrict timeout, size_t sigsetsize)
rt_sigtimedwait_time64(const sigset_t *restrict set, siginfo_t *restrict info, const struct timespec *restrict timeout, size_t sigsetsize)
rt_tgsigqueueinfo(pid_t tgid, pid_t tid, int sig, siginfo_t *info)
sigaltstack(const stack_t *_Nullable restrict ss, stack_t *_Nullable restrict old_ss)
signalfd(int fd, const sigset_t *mask, int flags)
signalfd4(int fd, const sigset_t *mask, int flags)
sigprocmask(int how, const sigset_t *_Nullable restrict set, sigset_t *_Nullable restrict oldset)
sigreturn()
tgkill(pid_t tgid, pid_t tid, int sig)
tkill(pid_t tid, int sig)
```

# File System
<!-- 
###############################################################################
##                                  file system                              ##
###############################################################################
-->

### File attributes
<!-- file attributes -->

```c
access(const char *pathname, int mode)
chown(const char *pathname, uid_t owner, gid_t group)
chown32(const char *pathname, uid_t owner, gid_t group)
chmod(const char *pathname, mode_t mode)
faccessat(int dirfd, const char *pathname, int mode, int flags)
faccessat2(int dirfd, const char *pathname, int mode, int flags)
fchmod(int fd, mode_t mode)
fchmodat(int dirfd, const char *pathname, mode_t mode, int flags)
fchown(int fd, uid_t owner, gid_t group)
fchown32(int fd, uid_t owner, gid_t group)
fchownat(int dirfd, const char *pathname, uid_t owner, gid_t group, int flags)
fstat(int fd, struct stat *statbuf)
fstat64(int fd, struct stat *statbuf)
fstatat64(int dirfd, const char *restrict pathname, struct stat *restrict statbuf, int flag)
futimesat(int dirfd, const char *pathname, const struct timeval times[2])
lchown(const char *pathname, uid_t owner, gid_t group)
lchown32(const char *pathname, uid_t owner, gid_t group)
_llseek(unsigned int fd, unsigned long offset_high, unsigned long offset_low, loff_t *result, unsigned int whence)
lseek(int fd, off_t offset, int whence)
lstat(const char *restrict pathname, struct stat *restrict statbuf)
lstat64(const char *restrict pathname, struct stat *restrict statbuf)
newfstatat(int dirfd, const char *restrict pathname, struct stat *restrict statbuf, int flag)
stat(const char *restrict pathname, struct stat *restrict statbuf)
stat64(const char *restrict pathname, struct stat *restrict statbuf)
statx(int dirfd, const char *restrict pathname, int flags,unsigned int mask, struct statx *restrict statxbuf)
umask(mode_t mask)
utime(const char *filename, const struct utimbuf *_Nullable times)
utimensat(int dirfd, const char *pathname, const struct timespec times[_Nullable 2], int flags)
utimensat_time64(int dirfd, const char *pathname, const struct timespec times[_Nullable 2], int flags)
utimes(const char *filename, const struct timeval times[_Nullable 2])
```

### Extended File Attributes
<!-- extended file attributes -->

```c
fgetxattr(int fd, const char *name,
                        void value[.size], size_t size)
flistxattr(int fd, char *_Nullable list, size_t size)
fremovexattr(int fd, const char *name)
fsetxattr(int fd, const char *name, const void value[.size], size_t size, int flags)
getxattr(const char *path, const char *name, void value[.size], size_t size)
lgetxattr(const char *path, const char *name, void value[.size], size_t size)
listxattr(const char *path, char *_Nullable list, size_t size)
llistxattr(const char *path, char *_Nullable list, size_t size)
lremovexattr(const char *path, const char *name)
lsetxattr(const char *path, const char *name, const void value[.size], size_t size, int flags)
removexattr(const char *path, const char *name)
setxattr(const char *path, const char *name, const void value[.size], size_t size, int flags)
```

### File Operations
<!-- file operations -->

```c
creat(const char *pathname, mode_t mode)
copy_file_range(int fd_in, off64_t *_Nullable off_in, int fd_out, off64_t *_Nullable off_out, size_t len, unsigned int flags)
fadvise64(int fd, off_t offset, size_t len, int advice)
fadvise64_64(int fd, off_t offset, loff_t len, int advice)
fallocate(int fd, int mode, off_t offset, off_t len)
fdatasync(int fd)
fsync(int fd)
ftruncate(int fd, off_t length)
ftruncate64(int fd, off_t length)
memfd_create(const char *name, unsigned int flags)
memfd_secret(unsigned int flags)
mknod(const char *pathname, mode_t mode, dev_t dev)
mknodat(int dirfd, const char *pathname, mode_t mode, dev_t dev)
name_to_handle_at(int dirfd, const char *pathname, struct file_handle *handle, int *mount_id, int flags)
open(const char *pathname, int flags)
openat(int dirfd, const char *pathname, int flags)
openat2(int dirfd, const char *pathname, const struct open_how *how, size_t size)
readahead(int fd, off64_t offset, size_t count)
rename(const char *oldpath, const char *newpath)
renameat(int olddirfd, const char *oldpath, int newdirfd, const char *newpath)
renameat2(int olddirfd, const char *oldpath, int newdirfd, const char *newpath, unsigned int flags)
sync_file_range(int fd, off64_t offset, off64_t nbytes, unsigned int flags)
truncate(const char *path, off_t length)
truncate64(const char *path, off_t length)
```

### Link Operations
<!-- link operations -->

```c
link(const char *oldpath, const char *newpath)
linkat(int olddirfd, const char *oldpath, int newdirfd, const char *newpath, int flags)
readlink(const char *restrict pathname, char *restrict buf, size_t bufsiz)
readlinkat(int dirfd, const char *restrict pathname, char *restrict buf, size_t bufsiz)
symlink(const char *target, const char *linkpath)
symlinkat(const char *target, int newdirfd, const char *linkpath)
unlink(const char *pathname)
unlinkat(int dirfd, const char *pathname, int flags)
```

### Directory Operations
<!-- directoriy operations -->

```c
chdir(const char *path)
fchdir(int fd)
getcwd(char buf[.size], size_t size)
getdents(unsigned int fd, struct linux_dirent *dirp, unsigned int count)
getdents64(unsigned int fd, struct linux_dirent *dirp, unsigned int count)
mkdir(const char *pathname, mode_t mode)
mkdirat(int dirfd, const char *pathname, mode_t mode)
rmdir(const char *pathname)
```

### File System Operations
<!-- file system operations -->

```c
fanotify_mark(int fanotify_fd, unsigned int flags, uint64_t mask, int dirfd, const char *_Nullable pathname)
fstatfs(int fd, struct statfs *buf)
fstatfs64(int fd, struct statfs *buf)
inotify_add_watch(int fd, const char *pathname, uint32_t mask)
inotify_init(void)
inotify_init1(int flags)
inotify_rm_watch(int fd, int wd)
statfs(const char *path, struct statfs *buf)
statfs64(const char *path, struct statfs *buf)
sync(void)
syncfs(int fd)
```

### File descriptors
<!-- file descriptors -->

```c
close(int fd)
close_range(unsigned int first, unsigned int last, unsigned int flags)
dup(int oldfd)
dup2(int oldfd, int newfd)
dup3(int oldfd, int newfd, int flag)
eventfd(unsigned int initval)
eventfd2(unsigned int initval, int flags)
fcntl(int fd, int cmd, ... /* arg */ )
fcntl64(int fd, int cmd, ... /* arg */ )
flock(int fd, int operation)
ioctl(int fd, unsigned long request, ...)
pidfd_open(pid_t pid, unsigned int flags)
pidfd_send_signal(int pidfd, int sig, siginfo_t *_Nullable info, unsigned int flags)
sendfile(int out_fd, int in_fd, off_t *_Nullable offset, size_t count)
sendfile64(int out_fd, int in_fd, off_t *_Nullable offset, size_t count)
```

### IO
<!-- IO -->

```c
io_cancel(aio_context_t ctx_id, struct iocb *iocb, struct io_event *result)
io_destroy(aio_context_t ctx_id)
io_getevents(aio_context_t ctx_id, long min_nr, long nr, struct io_event *events, struct timespec *timeout)
io_pgetevents(aio_context_t ctx_id, long, min_nr, long, nr, struct io_event __user *events, struct timespec __user *timeout, const struct __aio_sigset __user *usig)
io_pgetevents_time64(aio_context_t ctx_id, long, min_nr, long, nr, struct io_event __user *events, struct timespec __user *timeout, const struct __aio_sigset __user *usig)
ioprio_get(int which, int who)
ioprio_set(int which, int who, int ioprio)
io_setup(unsigned int nr_events, aio_context_t *ctx_idp)
io_submit(aio_context_t ctx_id, long nr, struct iocb **iocbpp)
io_uring_enter(unsigned int fd, unsigned int to_submit, unsigned int min_complete, unsigned int flags, sigset_t *sig)
io_uring_register(unsigned int fd, unsigned int opcode, void *arg, unsigned int nr_args)
io_uring_setup(u32 entries, struct io_uring_params *p)
```

# Programs
<!--
###############################################################################
##                                  programs                                 ##
###############################################################################
-->

### Processes
<!-- processes -->

```c
execve(const char *pathname, char *const _Nullable argv[], char *const _Nullable envp[])
execveat(int dirfd, const char *pathname, char *const _Nullable argv[], char *const _Nullable envp[], int flags)
exit(int status)
exit_group(int status)
fork(void)
getpriority(int which, id_t who)
process_mrelease(int pidfd, unsigned int flags)
restart_syscall(void)
setpriority(int which, id_t who, int prio)
vfork(void)
wait4(pid_t pid, int *_Nullable wstatus, int options, struct rusage *_Nullable rusage)
waitid(idtype_t idtype, id_t id, siginfo_t *infop, int options)
waitpid(pid_t pid, int *_Nullable wstatus, int options)
```

### Threads
<!-- threads -->

```c
capget(cap_user_header_t hdrp, cap_user_data_t datap)
capset(cap_user_header_t hdrp, const cap_user_data_t datap)
get_thread_area(struct user_desc *u_info)
gettid(void)
prctl(int option, unsigned long arg2, unsigned long arg3, unsigned long arg4, unsigned long arg5)
seccomp(unsigned int operation, unsigned int flags, void *args)
set_thread_area(struct user_desc *u_info)
set_tid_address(int *tidptr)
```

Scheduling:
<!-- threads.scheduling -->

```c
sched_getaffinity(pid_t pid, size_t cpusetsize, cpu_set_t *mask)
sched_getattr(pid_t pid, struct sched_attr *attr, unsigned int size, unsigned int flags)
sched_getparam(pid_t pid, struct sched_param *param)
sched_get_priority_max(int policy)
sched_get_priority_min(int policy)
sched_getscheduler(pid_t pid)
sched_rr_get_interval(pid_t pid, struct timespec *tp)
sched_rr_get_interval_time64(pid_t pid, struct timespec *tp)
sched_setaffinity(pid_t pid, size_t cpusetsize, const cpu_set_t *mask)
sched_setattr(pid_t pid, struct sched_attr *attr, unsigned int flags)
sched_setparam(pid_t pid, const struct sched_param *param)
sched_setscheduler(pid_t pid, int policy, const struct sched_param *para)
sched_yield(void)
```

### Memory
<!-- memory -->

```c
brk(void *addr)
madvise(void addr[.length], size_t length, int advice)
membarrier(int cmd, unsigned int flags, int cpu_id)
mincore(void addr[.length], size_t length, unsigned char *vec)
mmap(void addr[.length], size_t length, int prot, int flags, int fd, off_t offset)
mmap2(unsigned long addr, unsigned long length, unsigned long prot, unsigned long flags, unsigned long fd, unsigned long pgoffset)
mprotect(void addr[.len], size_t len, int prot)
mlock(const void addr[.len], size_t len)
mlock2(const void addr[.len], size_t len, unsigned int flags)
mlockall(int flags)
mremap(void old_address[.old_size], size_t old_size, size_t new_size, int flags, ... /* void *new_address */)
msync(void addr[.length], size_t length, int flags)
munlock(const void addr[.len], size_t len)
munlockall(void)
munmap(void addr[.length], size_t length)
remap_file_pages(void addr[.size], size_t size, int prot, size_t pgoff, int flags)
```

### Resources
<!-- resources -->

```c
getrlimit(int resource, struct rlimit *rlim)
getrusage(int who, struct rusage *usage)
prlimit64(pid_t pid, int resource,
                   const struct rlimit *_Nullable new_limit,
                   struct rlimit *_Nullable old_limit)
setrlimit(int resource, const struct rlimit *rlim)
ugetrlimit(int resource, struct rlimit *rlim)
```

### Identifiers
<!-- identifiers -->

```c
getegid(void)
getegid32(void)
geteuid(void)
geteuid32(void)
getgid(void)
getgid32(void)
getgroups(int size, gid_t list[])
getgroups32(int size, gid_t list[])
getpgid(pid_t pid)
getpgrp(void)
getpid(void)
getppid(void)
getresgid(gid_t *rgid, gid_t *egid, gid_t *sgid)
getresgid32(gid_t *rgid, gid_t *egid, gid_t *sgid)
getresuid(uid_t *ruid, uid_t *euid, uid_t *suid)
getresuid32(uid_t *ruid, uid_t *euid, uid_t *suid)
getsid(pid_t pid)
getuid(void)
getuid32(void)
setfsgid(gid_t fsgid)
setfsgid32(gid_t fsgid)
setfsuid(uid_t fsuid)
setfsuid32(uid_t fsuid)
setgid(gid_t gid)
setgid32(gid_t gid)
setgroups(size_t size, const gid_t *_Nullable list)
setgroups32(size_t size, const gid_t *_Nullable list)
setpgid(pid_t pid, pid_t pgid)
setregid(gid_t rgid, gid_t egid)
setregid32(gid_t rgid, gid_t egid)
setresgid(gid_t rgid, gid_t egid, gid_t sgid)
setresgid32(gid_t rgid, gid_t egid, gid_t sgid)
setresuid(uid_t ruid, uid_t euid, uid_t suid)
setresuid32(uid_t ruid, uid_t euid, uid_t suid)
setreuid(uid_t ruid, uid_t euid)
setreuid32(uid_t ruid, uid_t euid)
setsid(void)
setuid(uid_t uid)
setuid32(uid_t uid)
```

# Inter Process Communication
<!-- 
###############################################################################
##                          inter process communication                      ##
###############################################################################
-->

### Message Queue
<!-- message queue -->

```c
mq_getsetattr(mqd_t mqdes, const struct mq_attr *newattr, struct mq_attr *oldattr)
mq_notify(mqd_t mqdes, const struct sigevent *sevp)
mq_open(const char *name, int oflag)
mq_timedreceive(mqd_t mqdes, char *restrict msg_ptr[.msg_len], size_t msg_len, unsigned int *restrict msg_prio, const struct timespec *restrict abs_timeout)
mq_timedreceive_time64(mqd_t mqdes, char *restrict msg_ptr[.msg_len], size_t msg_len, unsigned int *restrict msg_prio, const struct timespec *restrict abs_timeout)
mq_timedsend(mqd_t mqdes, const char msg_ptr[.msg_len], size_t msg_len, unsigned int msg_prio, const struct timespec *abs_timeout)
mq_timedsend_time64(mqd_t mqdes, const char msg_ptr[.msg_len], size_t msg_len, unsigned int msg_prio, const struct timespec *abs_timeout)
mq_unlink(const char *name)
```

### System V
<!-- System V -->

```c
ipc(unsigned int call, int first, unsigned long second, unsigned long third, void *ptr, long fifth)
```

Message Queue:
<!-- System V.message queue -->

```c
msgctl(int msqid, int cmd, struct msqid_ds *buf)
msgget(key_t key, int msgflg)
msgrcv(int msqid, void msgp[.msgsz], size_t msgsz, long msgtyp, int msgflg)
msgsnd(int msqid, const void msgp[.msgsz], size_t msgsz, int msgflg)
```

Semaphores:
<!-- System V.semaphore -->

```c
semctl(int semid, int semnum, int cmd, ...)
semget(key_t key, int nsems, int semflg)
semop(int semid, struct sembuf *sops, size_t nsops)
semtimedop(int semid, struct sembuf *sops, size_t nsops, const struct timespec *_Nullable timeout)
semtimedop_time64(int semid, struct sembuf *sops, size_t nsops, const struct timespec *_Nullable timeout)
```

Shared Memory:
<!-- System V.shared memory -->

```c
shmat(int shmid, const void *_Nullable shmaddr, int shmflg)
shmctl(int shmid, int cmd, struct shmid_ds *buf)
shmdt(const void *shmaddr)
shmget(key_t key, size_t size, int shmflg)
```

### Pipes
<!-- pipes -->

```c
pipe(int pipefd[2])
pipe2(int pipefd[2], int flags)
splice(int fd_in, off64_t *_Nullable off_in, int fd_out, off64_t *_Nullable off_out, size_t len, unsigned int flags)
tee(int fd_in, int fd_out, size_t len, unsigned int flags)
vmsplice(int fd, const struct iovec *iov, size_t nr_segs, unsigned int flags)
```

### Futexes
<!-- futexes -->

```c
futex(uint32_t *uaddr, int futex_op, uint32_t val, const struct timespec *timeout,   /* or: uint32_t val2 */ uint32_t *uaddr2, uint32_t val3)
futex_time64(uint32_t *uaddr, int futex_op, uint32_t val, const struct timespec *timeout,   /* or: uint32_t val2 */ uint32_t *uaddr2, uint32_t val3)
futex_waitv(struct futex_waitv *waiters, unsigned int nr_futexes, unsigned int flags, struct timespec *timo)
get_robust_list(int pid, struct robust_list_head **head_ptr, size_t *len_ptr)
set_robust_list(struct robust_list_head *head, size_t len)
```

# Operations on File Descriptors
<!-- 
###############################################################################
##                        operations on file descriptors                     ##
###############################################################################
-->

### Select/poll/epoll
<!-- select/poll/epoll -->

```c
epoll_create(int size)
epoll_create1(int flags)
epoll_ctl(int epfd, int op, int fd, struct epoll_event *_Nullable event)
epoll_ctl_old(int epfd, int op, struct epoll_event *_Nullable event)
epoll_pwait(int epfd, struct epoll_event *events, int maxevents, int timeout, const sigset_t *_Nullable sigmask)
epoll_pwait2(int epfd, struct epoll_event *events, int maxevents, const struct timespec *_Nullable timeout, const sigset_t *_Nullable sigmask)
epoll_wait(int epfd, struct epoll_event *events, int maxevents, int timeout)
epoll_wait_old(int epfd, struct epoll_event *events, int maxevents)
_newselect(int nfds, fd_set *_Nullable restrict readfds, fd_set *_Nullable restrict writefds, fd_set *_Nullable restrict exceptfds, struct timeval *_Nullable restrict timeout)
poll(struct pollfd *fds, nfds_t nfds, int timeout)
ppoll(struct pollfd *fds, nfds_t nfds, const struct timespec *_Nullable tmo_p, const sigset_t *_Nullable sigmask)
ppoll_time64(struct pollfd *fds, nfds_t nfds, const struct timespec *_Nullable tmo_p, const sigset_t *_Nullable sigmask)
pselect6(int nfds, fd_set *_Nullable restrict readfds, fd_set *_Nullable restrict writefds, fd_set *_Nullable restrict exceptfds, const struct timespec *_Nullable restrict timeout, const sigset_t *_Nullable restrict sigmask)
pselect6_time64(int nfds, fd_set *_Nullable restrict readfds, fd_set *_Nullable restrict writefds, fd_set *_Nullable restrict exceptfds, const struct timespec *_Nullable restrict timeout, const sigset_t *_Nullable restrict sigmask)
select(int nfds, fd_set *_Nullable restrict readfds, fd_set *_Nullable restrict writefds, fd_set *_Nullable restrict exceptfds, struct timeval *_Nullable restrict timeout)
```

### Read
<!-- read -->

```c
pread64(int fd, void buf[.count], size_t count, off_t offset)
preadv(int fd, const struct iovec *iov, int iovcnt, off_t offset)
preadv2(int fd, const struct iovec *iov, int iovcnt, off_t offset, int flags)
read(int fd, void buf[.count], size_t count)
readv(int fd, const struct iovec *iov, int iovcnt)
```

### Write
<!-- write -->

```c
pwrite64(int fd, const void buf[.count], size_t count, off_t offset)
pwritev(int fd, const struct iovec *iov, int iovcnt, off_t offset)
pwritev2(int fd, const struct iovec *iov, int iovcnt, off_t offset, int flags)
write(int fd, const void buf[.count], size_t count)
writev(int fd, const struct iovec *iov, int iovcn)
```

# Miscellaneous
<!-- 
###############################################################################
##                                miscellaneous                              ##
###############################################################################
-->

```c
getcpu(unsigned int *_Nullable cpu, unsigned int *_Nullable node)
getrandom(void buf[.buflen], size_t buflen, unsigned int flags)
landlock_add_rule(int ruleset_fd, enum landlock_rule_type rule_type, const void *rule_attr, uint32_t flags)
landlock_create_ruleset(const struct landlock_ruleset_attr *attr, size_t size , uint32_t flags)
landlock_restrict_self(int ruleset_fd, uint32_t flags)
pkey_alloc(unsigned int flags, unsigned int access_rights)
pkey_free(int pkey)
pkey_mprotect(void addr[.len], size_t len, int prot, int pkey)
rseq(struct rseq *rseq, uint32_t rseq_len, int flags, uint32_t sig )
sysinfo(struct sysinfo *info)
uname(struct utsname *buf)
```