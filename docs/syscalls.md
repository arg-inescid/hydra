<style>
    sb { color: steelblue } /* for operations that expose the underlying system */
    lg { color: lightgreen } /* for operations with file descriptors */
    o { color: orange } /* for operations with pathnames and filenames */
    t: { color: tomato } /* for operations with kernel data structures */
    x { color: gray } /* arguments */
</style>

<!-- time -->

## Time

adjtimex<x>(struct timex \*buf)</x><br>
clock_adjtime<x>(clockid_t clk_id, struct timex \*buf)</x><br>
clock_adjtime64<x>(clockid_t clk_id, struct timex \*buf)</x><br>
clock_getres<x>(clockid_t clockid, struct timespec \*_Nullable res)</x><br>
clock_getres_time64<x>(clockid_t clockid, struct timespec \*_Nullable res)</x><br>
clock_gettime<x>(clockid_t clockid, struct timespec \*tp)</x><br>
clock_gettime64<x>(clockid_t clockid, struct timespec \*tp)</x><br>
clock_nanosleep<x>(clockid_t clockid, int flags, const struct timespec \*request, struct timespec \*_Nullable remain)</x><br>
clock_nanosleep_time64<x>(clockid_t clockid, int flags, const struct timespec \*request, struct timespec \*_Nullable remain)</x><br>
getitimer<x>(int which, struct itimerval \*curr_value)</x><br>
gettimeofday<x>(struct timeval \*restrict tv, struct timezone \*_Nullable restrict tz)</x><br>
nanosleep<x>(const struct timespec \*req, struct timespec \*_Nullable rem)</x><br>
setitimer<x>(int which, const struct itimerval \*restrict new_value, struct itimerval \*_Nullable restrict old_value)</x><br>
time<x>(time_t \*_Nullable tloc)</x><br>
timer_create<x>(clockid_t clockid, struct sigevent \*_Nullable restrict sevp, timer_t \*restrict timerid)</x><br>
timer_delete<x>(timer_t timerid)</x><br>
timer_getoverrun<x>(timer_t timerid)</x><br>
timer_gettime<x>(timer_t timerid, struct itimerspec \*curr_value)</x><br>
timer_gettime64<x>(timer_t timerid, struct itimerspec \*curr_value)</x><br>
timer_settime<x>(timer_t timerid, int flags, const struct itimerspec \*restrict new_value, struct itimerspec \*_Nullable restrict old_value)</x><br>
timer_settime64<x>(timer_t timerid, int flags, const struct itimerspec \*restrict new_value, struct itimerspec \*_Nullable restrict old_value)</x><br>
timerfd_create<x>(int clockid, int flags)</x><br>
timerfd_gettime<x>(int fd, struct itimerspec \*curr_value)</x><br>
timerfd_gettime64<x>(int fd, struct itimerspec \*curr_value)</x><br>
timerfd_settime<x>(int fd, int flags, const struct itimerspec \*new_value, struct itimerspec \*_Nullable old_value)</x><br>
timerfd_settime64<x>(int fd, int flags, const struct itimerspec \*new_value, struct itimerspec \*_Nullable old_value)</x><br>
times<x>(struct tms \*buf)</x><br>

## Network
<!-- network -->

${\color{lightgreen}accept}$<br>
<lg>accept</lg><x>(int sockfd, struct sockaddr \*_Nullable restrict addr, socklen_t \*_Nullable restrict addrlen)</x><br>
<lg>accept4</lg><x>(int sockfd, struct sockaddr \*_Nullable restrict addr, socklen_t \*_Nullable restrict addrlen, int flags)</x><br>
<lg>bind</lg><x>(int sockfd, const struct sockaddr \*addr, socklen_t addrlen)</x><br>
<lg>connect</lg><x>(int sockfd, const struct sockaddr \*addr, socklen_t addrlen)</x><br>
<lg>getpeername</lg><x>(int sockfd, struct sockaddr \*restrict addr, socklen_t \*restrict addrlen)</x><br>
<lg>getsockname</lg><x>(int sockfd, struct sockaddr \*restrict addr, socklen_t \*restrict addrlen)</x><br>
<lg>getsockopt</lg><x>(int sockfd, int level, int optname, void optval[restrict \*.optlen], socklen_t \*restrict optlen)</x><br>
<lg>listen</lg><x>(int sockfd, int backlog)</x><br>
<lg>recv</lg><x>(int sockfd, void buf[.len], size_t len, int flags)</x><br>
<lg>recvfrom</lg><x>(int sockfd, void buf[restrict .len], size_t len, int flags, struct sockaddr \*_Nullable restrict src_addr, socklen_t \*_Nullable restrict addrlen)</x><br>
<lg>recvmmsg</lg><x>(int sockfd, struct mmsghdr \*msgvec, unsigned int vlen, int flags, struct timespec \*timeout)</x><br>
<lg>recvmmsg_time64</lg><x>(int sockfd, struct mmsghdr \*msgvec, unsigned int vlen, int flags, struct timespec \*timeout)</x><br>
<lg>recvmsg</lg><x>(int sockfd, struct msghdr \*msg, int flags)</x><br>
<lg>send</lg><x>(int sockfd, const void buf[.len], size_t len, int flags)</x><br>
<lg>sendmmsg</lg><x>(int sockfd, struct mmsghdr \*msgvec, unsigned int vlen, int flags)</x><br>
<lg>sendmsg</lg><x>(int sockfd, const struct msghdr \*msg, int flags)</x><br>
<lg>sendto</lg><x>(int sockfd, const void buf[.len], size_t len, int flags, const struct sockaddr \*dest_addr, socklen_t addrlen)</x><br>
<lg>setsockopt</lg><x>(int sockfd, int level, int optname, const void optval[.optlen], socklen_t optlen)</x><br>
<lg>shutdown</lg><x>(int sockfd, int how)</x><br>
socketcall<x>(int call, unsigned long \*args)</x><br>
socketpair<x>(int domain, int type, int protocol, int sv[2])</x><br>

## Signals
<!-- signals -->

alarm<x>(unsigned int seconds)</x><br>
kill<x>(pid_t pid, int sig)</x><br>
pause<x>(void)</x><br>
rt_sigaction<x>(int signum, const struct sigaction \*_Nullable restrict act, struct sigaction \*_Nullable restrict oldact, size_t sigsetsize)</x><br>
rt_sigpending<x>(sigset_t \*set, size_t sigsetsize)</x><br>
rt_sigprocmask<x>(int how, const kernel_sigset_t \*_Nullable set, kernel_sigset_t \*_Nullable oldset, size_t sigsetsize)</x><br>
rt_sigqueueinfo<x>(pid_t tgid, int sig, siginfo_t \*info)</x><br>
rt_sigreturn<x>()</x><br>
rt_sigsuspend<x>(const sigset_t \*mask, size_t sigsetsize)</x><br>
rt_sigtimedwait<x>(const sigset_t \*restrict set, siginfo_t \*restrict info, const struct timespec \*restrict timeout, size_t sigsetsize)</x><br>
rt_sigtimedwait_time64<x>(const sigset_t \*restrict set, siginfo_t \*restrict info, const struct timespec \*restrict timeout, size_t sigsetsize)</x><br>
rt_tgsigqueueinfo<x>(pid_t tgid, pid_t tid, int sig, siginfo_t \*info)</x><br>
sigaltstack<x>(const stack_t \*_Nullable restrict ss, stack_t \*_Nullable restrict old_ss)</x><br>
signalfd<x>(int fd, const sigset_t \*mask, int flags)</x><br>
signalfd4<x>(int fd, const sigset_t \*mask, int flags)</x><br>
sigprocmask<x>(int how, const sigset_t \*_Nullable restrict set, sigset_t \*_Nullable restrict oldset)</x><br>
sigreturn<x>()</x><br>
tgkill<x>(pid_t tgid, pid_t tid, int sig)</x><br>
tkill<x>(pid_t tid, int sig)</x><br>

# File System
<!-- 
###############################################################################
##                                  file system                              ##
###############################################################################
-->

## File attributes
<!-- file attributes -->

<o>access</o><x>(const char \*pathname, int mode)</x><br>
<o>chown</o><x>(const char \*pathname, uid_t owner, gid_t group)</x><br>
<o>chown32</o><x>(const char \*pathname, uid_t owner, gid_t group)</x><br>
<o>chmod</o><x>(const char \*pathname, mode_t mode)</x><br>
<o>faccessat</o><x>(int dirfd, const char \*pathname, int mode, int flags)</x><br>
<o>faccessat2</o><x>(int dirfd, const char \*pathname, int mode, int flags)</x><br>
<lg>fchmod</lg><x>(int fd, mode_t mode)</x><br>
<o>fchmodat</o><x>(int dirfd, const char \*pathname, mode_t mode, int flags)</x><br>
<lg>fchown</lg><x>(int fd, uid_t owner, gid_t group)</x><br>
<lg>fchown32</lg><x>(int fd, uid_t owner, gid_t group)</x><br>
<o>fchownat</o><x>(int dirfd, const char \*pathname, uid_t owner, gid_t group, int flags)</x><br>
<lg>fstat</lg><x>(int fd, struct stat \*statbuf)</x><br>
<lg>fstat64</lg><x>(int fd, struct stat \*statbuf)</x><br>
<o>fstatat64</o><x>(int dirfd, const char \*restrict pathname, struct stat \*restrict statbuf, int flag)</x><br>
<o>futimesat</o><x>(int dirfd, const char \*pathname, const struct timeval times[2])</x><br>
<o>lchown</o><x>(const char \*pathname, uid_t owner, gid_t group)</x><br>
<o>lchown32</o><x>(const char \*pathname, uid_t owner, gid_t group)</x><br>
<lg>_llseek</lg><x>(unsigned int fd, unsigned long offset_high, unsigned long offset_low, loff_t \*result, unsigned int whence)</x><br>
<lg>lseek</lg><x>(int fd, off_t offset, int whence)</x><br>
<o>lstat</o><x>(const char \*restrict pathname, struct stat \*restrict statbuf)</x><br>
<o>lstat64</o><x>(const char \*restrict pathname, struct stat \*restrict statbuf)</x><br>
<o>newfstatat</o><x>(int dirfd, const char \*restrict pathname, struct stat \*restrict statbuf, int flag)</x><br>
<o>stat</o><x>(const char \*restrict pathname, struct stat \*restrict statbuf)</x><br>
<o>stat64</o><x>(const char \*restrict pathname, struct stat \*restrict statbuf)</x><br>
<lg>statx</lg><x>(int dirfd, const char \*restrict pathname, int flags,unsigned int mask, struct statx \*restrict statxbuf)</x><br>
umask<x>(mode_t mask)</x><br>
<o>utime</o><x>(const char \*filename, const struct utimbuf \*_Nullable times)</x><br>
<o>utimensat</o><x>(int dirfd, const char \*pathname, const struct timespec times[_Nullable 2], int flags)</x><br>
<o>utimensat_time64</o><x>(int dirfd, const char \*pathname, const struct timespec times[_Nullable 2], int flags)</x><br>
<o>utimes</o><x>(const char \*filename, const struct timeval times[_Nullable 2])</x><br>

## Extended File Attributes
<!-- extended file attributes -->

<lg>fgetxattr</lg><x>(int fd, const char \*name, void value[.size], size_t size)</x><br>
<lg>flistxattr</lg><x>(int fd, char \*_Nullable list, size_t size)</x><br>
<lg>fremovexattr</lg><x>(int fd, const char \*name)</x><br>
<lg>fsetxattr</lg><x>(int fd, const char \*name, const void value[.size], size_t size, int flags)</x><br>
<o>getxattr</o><x>(const char \*path, const char \*name, void value[.size], size_t size)</x><br>
<o>lgetxattr</o><x>(const char \*path, const char \*name, void value[.size], size_t size)</x><br>
<o>listxattr</o><x>(const char \*path, char \*_Nullable list, size_t size)</x><br>
<o>llistxattr</o><x>(const char \*path, char \*_Nullable list, size_t size)</x><br>
<o>lremovexattr</o><x>(const char \*path, const char \*name)</x><br>
<o>lsetxattr</o><x>(const char \*path, const char \*name, const void value[.size], size_t size, int flags)</x><br>
<o>removexattr</o><x>(const char \*path, const char \*name)</x><br>
<o>setxattr</o><x>(const char \*path, const char \*name, const void value[.size], size_t size, int flags)</x><br>

## File Operations
<!-- file operations -->

<o>creat</o><x>(const char \*pathname, mode_t mode)</x><br>
<lg>copy_file_range</lg><x>(int fd_in, off64_t \*_Nullable off_in, int fd_out, off64_t \*_Nullable off_out, size_t len, unsigned int flags)</x><br>
<lg>fadvise64</lg><x>(int fd, off_t offset, size_t len, int advice)</x><br>
<lg>fadvise64_64</lg><x>(int fd, off_t offset, loff_t len, int advice)</x><br>
<lg>fallocate</lg><x>(int fd, int mode, off_t offset, off_t len)</x><br>
<lg>fdatasync</lg><x>(int fd)</x><br>
<lg>fsync</lg><x>(int fd)</x><br>
<lg>ftruncate</lg><x>(int fd, off_t length)</x><br>
<lg>ftruncate64</lg><x>(int fd, off_t length)</x><br>
memfd_create<x>(const char \*name, unsigned int flags)</x><br>
memfd_secret<x>(unsigned int flags)</x><br>
<o>mknod</o><x>(const char \*pathname, mode_t mode, dev_t dev)</x><br>
<o>mknodat</o><x>(int dirfd, const char \*pathname, mode_t mode, dev_t dev)</x><br>
<o>name_to_handle_at</o><x>(int dirfd, const char \*pathname, struct file_handle \*handle, int \*mount_id, int flags)</x><br>
<o>open</o><x>(const char \*pathname, int flags)</x><br>
<o>openat</o><x>(int dirfd, const char \*pathname, int flags)</x><br>
<o>openat2</o><x>(int dirfd, const char \*pathname, const struct open_how \*how, size_t size)</x><br>
<lg>readahead</lg><x>(int fd, off64_t offset, size_t count)</x><br>
<o>rename</o><x>(const char \*oldpath, const char \*newpath)</x><br>
<o>renameat</o><x>(int olddirfd, const char \*oldpath, int newdirfd, const char \*newpath)</x><br>
<o>renameat2</o><x>(int olddirfd, const char \*oldpath, int newdirfd, const char \*newpath, unsigned int flags)</x><br>
<lg>sync_file_range</lg><x>(int fd, off64_t offset, off64_t nbytes, unsigned int flags)</x><br>
<o>truncate</o><x>(const char \*path, off_t length)</x><br>
<o>truncate64</o><x>(const char \*path, off_t length)</x><br>

## Link Operations
<!-- link operations -->

<o>link</o><x>(const char \*oldpath, const char \*newpath)</x><br>
<o>linkat</o><x>(int olddirfd, const char \*oldpath, int newdirfd, const char \*newpath, int flags)</x><br>
<o>readlink</o><x>(const char \*restrict pathname, char \*restrict buf, size_t bufsiz)</x><br>
<o>readlinkat</o><x>(int dirfd, const char \*restrict pathname, char \*restrict buf, size_t bufsiz)</x><br>
<o>symlink</o><x>(const char \*target, const char \*linkpath)</x><br>
<o>symlinkat</o><x>(const char \*target, int newdirfd, const char \*linkpath)</x><br>
<o>unlink</o><x>(const char \*pathname)</x><br>
<o>unlinkat</o><x>(int dirfd, const char \*pathname, int flags)</x><br>

## Directory Operations
<!-- directoriy operations -->

<o>chdir</o><x>(const char \*path)</x><br>
<lg>fchdir</lg><x>(int fd)</x><br>
getcwd<x>(char buf[.size], size_t size)</x><br>
<lg>getdents</lg><x>(unsigned int fd, struct linux_dirent \*dirp, unsigned int count)</x><br>
<lg>getdents64</lg><x>(unsigned int fd, struct linux_dirent \*dirp, unsigned int count)</x><br>
<o>mkdir</o><x>(const char \*pathname, mode_t mode)</x><br>
<o>mkdirat</o><x>(int dirfd, const char \*pathname, mode_t mode)</x><br>
<o>rmdir</o><x>(const char \*pathname)</x><br>

## File System Operations
<!-- file system operations -->

<lg>fanotify_mark</lg><x>(int fanotify_fd, unsigned int flags, uint64_t mask, int dirfd, const char \*_Nullable pathname)</x><br>
<sb>fstatfs</sb><x>(int fd, struct statfs \*buf)</x><br>
<sb>fstatfs64</sb><x>(int fd, struct statfs \*buf)</x><br>
<lg>inotify_add_watch</lg><x>(int fd, const char \*pathname, uint32_t mask)</x><br>
inotify_init<x>(void)</x><br>
inotify_init1<x>(int flags)</x><br>
<lg>inotify_rm_watch</lg><x>(int fd, int wd)</x><br>
<sb>statfs</sb><x>(const char \*path, struct statfs \*buf)</x><br>
<sb>statfs64</sb><x>(const char \*path, struct statfs \*buf)</x><br>
sync<x>(void)</x><br>
<sb>syncfs</sb><x>(int fd)</x><br>

## File descriptors
<!-- file descriptors -->

<lg>close</lg><x>(int fd)</x><br>
<lg>close_range</lg><x>(unsigned int first, unsigned int last, unsigned int flags)</x><br>
<lg>dup</lg><x>(int oldfd)</x><br>
<lg>dup2</lg><x>(int oldfd, int newfd)</x><br>
<lg>dup3</lg><x>(int oldfd, int newfd, int flag)</x><br>
eventfd<x>(unsigned int initval)</x><br>
eventfd2<x>(unsigned int initval, int flags)</x><br>
<lg>fcntl</lg><x>(int fd, int cmd, ... /\* arg \*/ )</x><br>
<lg>fcntl64</lg><x>(int fd, int cmd, ... /\* arg \*/ )</x><br>
<lg>flock</lg><x>(int fd, int operation)</x><br>
<lg>ioctl</lg><x>(int fd, unsigned long request, ...)</x><br>
pidfd_open<x>(pid_t pid, unsigned int flags)</x><br>
pidfd_send_signal<x>(int pidfd, int sig, siginfo_t \*_Nullable info, unsigned int flags)</x><br>
<lg>sendfile</lg><x>(int out_fd, int in_fd, off_t \*_Nullable offset, size_t count)</x><br>
<lg>sendfile64</lg><x>(int out_fd, int in_fd, off_t \*_Nullable offset, size_t count)</x><br>

## IO
<!-- IO -->

io_cancel<x>(aio_context_t ctx_id, struct iocb \*iocb, struct io_event \*result)</x><br>
io_destroy<x>(aio_context_t ctx_id)</x><br>
io_getevents<x>(aio_context_t ctx_id, long min_nr, long nr, struct io_event \*events, struct timespec \*timeout)</x><br>
io_pgetevents<x>(aio_context_t ctx_id, long, min_nr, long, nr, struct io_event __user \*events, struct timespec __user \*timeout, const struct __aio_sigset __user \*usig)</x><br>
io_pgetevents_time64<x>(aio_context_t ctx_id, long, min_nr, long, nr, struct io_event __user \*events, struct timespec __user \*timeout, const struct __aio_sigset __user \*usig)</x><br>
ioprio_get<x>(int which, int who)</x><br>
ioprio_set<x>(int which, int who, int ioprio)</x><br>
io_setup<x>(unsigned int nr_events, aio_context_t \*ctx_idp)</x><br>
io_submit<x>(aio_context_t ctx_id, long nr, struct iocb \*\*iocbpp)</x><br>
<lg>io_uring_enter</lg><x>(unsigned int fd, unsigned int to_submit, unsigned int min_complete, unsigned int flags, sigset_t \*sig)</x><br>
<lg>io_uring_register</lg><x>(unsigned int fd, unsigned int opcode, void \*arg, unsigned int nr_args)</x><br>
io_uring_setup<x>(u32 entries, struct io_uring_params \*p)</x><br>

# Programs
<!--
###############################################################################
##                                  programs                                 ##
###############################################################################
-->

## Processes
<!-- processes -->

<o>execve</o><x>(const char \*pathname, char \*const _Nullable argv[], char \*const _Nullable envp[])</x><br>
<o>execveat</o><x>(int dirfd, const char \*pathname, char \*const _Nullable argv[], char \*const _Nullable envp[], int flags)</x><br>
exit<x>(int status)</x><br>
exit_group<x>(int status)</x><br>
fork<x>(void)</x><br>
getpriority<x>(int which, id_t who)</x><br>
process_mrelease<x>(int pidfd, unsigned int flags)</x><br>
restart_syscall<x>(void)</x><br>
setpriority<x>(int which, id_t who, int prio)</x><br>
vfork<x>(void)</x><br>
wait4<x>(pid_t pid, int \*_Nullable wstatus, int options, struct rusage \*_Nullable rusage)</x><br>
waitid<x>(idtype_t idtype, id_t id, siginfo_t \*infop, int options)</x><br>
waitpid<x>(pid_t pid, int \*_Nullable wstatus, int options)</x><br>

## Threads
<!-- threads -->

capget<x>(cap_user_header_t hdrp, cap_user_data_t datap)</x><br>
capset<x>(cap_user_header_t hdrp, const cap_user_data_t datap)</x><br>
get_thread_area<x>(struct user_desc \*u_info)</x><br>
gettid<x>(void)</x><br>
prctl<x>(int option, unsigned long arg2, unsigned long arg3, unsigned long arg4, unsigned long arg5)</x><br>
seccomp<x>(unsigned int operation, unsigned int flags, void \*args)</x><br>
set_thread_area<x>(struct user_desc \*u_info)</x><br>
set_tid_address<x>(int \*tidptr)</x><br>

### Scheduling:
<!-- threads.scheduling -->

sched_getaffinity<x>(pid_t pid, size_t cpusetsize, cpu_set_t \*mask)</x><br>
sched_getattr<x>(pid_t pid, struct sched_attr \*attr, unsigned int size, unsigned int flags)</x><br>
sched_getparam<x>(pid_t pid, struct sched_param \*param)</x><br>
sched_get_priority_max<x>(int policy)</x><br>
sched_get_priority_min<x>(int policy)</x><br>
sched_getscheduler<x>(pid_t pid)</x><br>
sched_rr_get_interval<x>(pid_t pid, struct timespec \*tp)</x><br>
sched_rr_get_interval_time64<x>(pid_t pid, struct timespec \*tp)</x><br>
sched_setaffinity<x>(pid_t pid, size_t cpusetsize, const cpu_set_t \*mask)</x><br>
sched_setattr<x>(pid_t pid, struct sched_attr \*attr, unsigned int flags)</x><br>
sched_setparam<x>(pid_t pid, const struct sched_param \*param)</x><br>
sched_setscheduler<x>(pid_t pid, int policy, const struct sched_param \*para)</x><br>
sched_yield<x>(void)</x><br>

## Memory
<!-- memory -->

brk<x>(void \*addr)</x><br>
madvise<x>(void addr[.length], size_t length, int advice)</x><br>
membarrier<x>(int cmd, unsigned int flags, int cpu_id)</x><br>
mincore<x>(void addr[.length], size_t length, unsigned char \*vec)</x><br>
<lg>mmap</lg><x>(void addr[.length], size_t length, int prot, int flags, int fd, off_t offset)</x><br>
<lg>mmap2</lg><x>(unsigned long addr, unsigned long length, unsigned long prot, unsigned long flags, unsigned long fd, unsigned long pgoffset)</x><br>
mprotect<x>(void addr[.len], size_t len, int prot)</x><br>
mlock<x>(const void addr[.len], size_t len)</x><br>
mlock2<x>(const void addr[.len], size_t len, unsigned int flags)</x><br>
mlockall<x>(int flags)</x><br>
mremap<x>(void old_address[.old_size], size_t old_size, size_t new_size, int flags, ... /\* void \*new_address \*/)</x><br>
msync<x>(void addr[.length], size_t length, int flags)</x><br>
munlock<x>(const void addr[.len], size_t len)</x><br>
munlockall<x>(void)</x><br>
munmap<x>(void addr[.length], size_t length)</x><br>
remap_file_pages<x>(void addr[.size], size_t size, int prot, size_t pgoff, int flags)</x><br>

## Resources
<!-- resources -->

getrlimit<x>(int resource, struct rlimit \*rlim)</x><br>
getrusage<x>(int who, struct rusage \*usage)</x><br>
prlimit64<x>(pid_t pid, int resource, const struct rlimit \*_Nullable new_limit, struct rlimit \*_Nullable old_limit)</x><br>
setrlimit<x>(int resource, const struct rlimit \*rlim)</x><br>
ugetrlimit<x>(int resource, struct rlimit \*rlim)</x><br>

## Identifiers
<!-- identifiers -->

getegid<x>(void)</x><br>
getegid32<x>(void)</x><br>
geteuid<x>(void)</x><br>
geteuid32<x>(void)</x><br>
getgid<x>(void)</x><br>
getgid32<x>(void)</x><br>
getgroups<x>(int size, gid_t list[])</x><br>
getgroups32<x>(int size, gid_t list[])</x><br>
getpgid<x>(pid_t pid)</x><br>
getpgrp<x>(void)</x><br>
getpid<x>(void)</x><br>
getppid<x>(void)</x><br>
getresgid<x>(gid_t \*rgid, gid_t \*egid, gid_t \*sgid)</x><br>
getresgid32<x>(gid_t \*rgid, gid_t \*egid, gid_t \*sgid)</x><br>
getresuid<x>(uid_t \*ruid, uid_t \*euid, uid_t \*suid)</x><br>
getresuid32<x>(uid_t \*ruid, uid_t \*euid, uid_t \*suid)</x><br>
getsid<x>(pid_t pid)</x><br>
getuid<x>(void)</x><br>
getuid32<x>(void)</x><br>
setfsgid<x>(gid_t fsgid)</x><br>
setfsgid32<x>(gid_t fsgid)</x><br>
setfsuid<x>(uid_t fsuid)</x><br>
setfsuid32<x>(uid_t fsuid)</x><br>
setgid<x>(gid_t gid)</x><br>
setgid32<x>(gid_t gid)</x><br>
setgroups<x>(size_t size, const gid_t \*_Nullable list)</x><br>
setgroups32<x>(size_t size, const gid_t \*_Nullable list)</x><br>
setpgid<x>(pid_t pid, pid_t pgid)</x><br>
setregid<x>(gid_t rgid, gid_t egid)</x><br>
setregid32<x>(gid_t rgid, gid_t egid)</x><br>
setresgid<x>(gid_t rgid, gid_t egid, gid_t sgid)</x><br>
setresgid32<x>(gid_t rgid, gid_t egid, gid_t sgid)</x><br>
setresuid<x>(uid_t ruid, uid_t euid, uid_t suid)</x><br>
setresuid32<x>(uid_t ruid, uid_t euid, uid_t suid)</x><br>
setreuid<x>(uid_t ruid, uid_t euid)</x><br>
setreuid32<x>(uid_t ruid, uid_t euid)</x><br>
setsid<x>(void)</x><br>
setuid<x>(uid_t uid)</x><br>
setuid32<x>(uid_t uid)</x><br>

# Inter Process Communication
<!-- 
###############################################################################
##                          inter process communication                      ##
###############################################################################
-->

## Message Queue
<!-- message queue -->

mq_getsetattr<x>(mqd_t mqdes, const struct mq_attr \*newattr, struct mq_attr \*oldattr)</x><br>
mq_notify<x>(mqd_t mqdes, const struct sigevent \*sevp)</x><br>
mq_open<x>(const char \*name, int oflag)</x><br>
mq_timedreceive<x>(mqd_t mqdes, char \*restrict msg_ptr[.msg_len], size_t msg_len, unsigned int \*restrict msg_prio, const struct timespec \*restrict abs_timeout)</x><br>
mq_timedreceive_time64<x>(mqd_t mqdes, char \*restrict msg_ptr[.msg_len], size_t msg_len, unsigned int \*restrict msg_prio, const struct timespec \*restrict abs_timeout)</x><br>
mq_timedsend<x>(mqd_t mqdes, const char msg_ptr[.msg_len], size_t msg_len, unsigned int msg_prio, const struct timespec \*abs_timeout)</x><br>
mq_timedsend_time64<x>(mqd_t mqdes, const char msg_ptr[.msg_len], size_t msg_len, unsigned int msg_prio, const struct timespec \*abs_timeout)</x><br>
mq_unlink<x>(const char \*name)</x><br>

## System V
<!-- System V -->

ipc<x>(unsigned int call, int first, unsigned long second, unsigned long third, void \*ptr, long fifth)</x><br>

### Message Queue:
<!-- System V.message queue -->

msgctl<x>(int msqid, int cmd, struct msqid_ds \*buf)</x><br>
msgget<x>(key_t key, int msgflg)</x><br>
msgrcv<x>(int msqid, void msgp[.msgsz], size_t msgsz, long msgtyp, int msgflg)</x><br>
msgsnd<x>(int msqid, const void msgp[.msgsz], size_t msgsz, int msgflg)</x><br>

### Semaphores:
<!-- System V.semaphore -->

semctl<x>(int semid, int semnum, int cmd, ...)</x><br>
semget<x>(key_t key, int nsems, int semflg)</x><br>
semop<x>(int semid, struct sembuf \*sops, size_t nsops)</x><br>
semtimedop<x>(int semid, struct sembuf \*sops, size_t nsops, const struct timespec \*_Nullable timeout)</x><br>
semtimedop_time64<x>(int semid, struct sembuf \*sops, size_t nsops, const struct timespec \*_Nullable timeout)</x><br>

### Shared Memory:
<!-- System V.shared memory -->

shmat<x>(int shmid, const void \*_Nullable shmaddr, int shmflg)</x><br>
shmctl<x>(int shmid, int cmd, struct shmid_ds \*buf)</x><br>
shmdt<x>(const void \*shmaddr)</x><br>
shmget<x>(key_t key, size_t size, int shmflg)</x><br>

## Pipes
<!-- pipes -->

pipe<x>(int pipefd[2])</x><br>
pipe2<x>(int pipefd[2], int flags)</x><br>
<lg>splice</lg><x>(int fd_in, off64_t \*_Nullable off_in, int fd_out, off64_t \*_Nullable off_out, size_t len, unsigned int flags)</x><br>
<lg>tee</lg><x>(int fd_in, int fd_out, size_t len, unsigned int flags)</x><br>
<lg>vmsplice</lg><x>(int fd, const struct iovec \*iov, size_t nr_segs, unsigned int flags)</x><br>

## Futexes
<!-- futexes -->

futex<x>(uint32_t \*uaddr, int futex_op, uint32_t val, const struct timespec \*timeout,   /\* or: uint32_t val2 \*/ uint32_t \*uaddr2, uint32_t val3)</x><br>
futex_time64<x>(uint32_t \*uaddr, int futex_op, uint32_t val, const struct timespec \*timeout,   /\* or: uint32_t val2 \*/ uint32_t \*uaddr2, uint32_t val3)</x><br>
futex_waitv<x>(struct futex_waitv \*waiters, unsigned int nr_futexes, unsigned int flags, struct timespec \*timo)</x><br>
get_robust_list<x>(int pid, struct robust_list_head \*\*head_ptr, size_t \*len_ptr)</x><br>
set_robust_list<x>(struct robust_list_head \*head, size_t len)</x><br>

# Operations on File Descriptors
<!-- 
###############################################################################
##                        operations on file descriptors                     ##
###############################################################################
-->

## Select/poll/epoll
<!-- select/poll/epoll -->

epoll_create<x>(int size)</x><br>
epoll_create1<x>(int flags)</x><br>
<lg>epoll_ctl</lg><x>(int epfd, int op, int fd, struct epoll_event \*_Nullable event)</x><br>
<lg>epoll_ctl_old</lg><x>(int epfd, int op, struct epoll_event \*_Nullable event)</x><br>
<lg>epoll_pwait</lg><x>(int epfd, struct epoll_event \*events, int maxevents, int timeout, const sigset_t \*_Nullable sigmask)</x><br>
<lg>epoll_pwait2</lg><x>(int epfd, struct epoll_event \*events, int maxevents, const struct timespec \*_Nullable timeout, const sigset_t \*_Nullable sigmask)</x><br>
<lg>epoll_wait</lg><x>(int epfd, struct epoll_event \*events, int maxevents, int timeout)</x><br>
<lg>epoll_wait_old</lg><x>(int epfd, struct epoll_event \*events, int maxevents)</x><br>
<lg>_newselect</lg><x>(int nfds, fd_set \*_Nullable restrict readfds, fd_set \*_Nullable restrict writefds, fd_set \*_Nullable restrict exceptfds, struct timeval \*_Nullable restrict timeout)</x><br>
<lg>poll</lg><x>(struct pollfd \*fds, nfds_t nfds, int timeout)</x><br>
<lg>ppoll</lg><x>(struct pollfd \*fds, nfds_t nfds, const struct timespec \*_Nullable tmo_p, const sigset_t \*_Nullable sigmask)</x><br>
<lg>ppoll_time64</lg><x>(struct pollfd \*fds, nfds_t nfds, const struct timespec \*_Nullable tmo_p, const sigset_t \*_Nullable sigmask)</x><br>
<lg>pselect6</lg><x>(int nfds, fd_set \*_Nullable restrict readfds, fd_set \*_Nullable restrict writefds, fd_set \*_Nullable restrict exceptfds, const struct timespec \*_Nullable restrict timeout, const sigset_t \*_Nullable restrict sigmask)</x><br>
<lg>pselect6_time64</lg><x>(int nfds, fd_set \*_Nullable restrict readfds, fd_set \*_Nullable restrict writefds, fd_set \*_Nullable restrict exceptfds, const struct timespec \*_Nullable restrict timeout, const sigset_t \*_Nullable restrict sigmask)</x><br>
<lg>select</lg><x>(int nfds, fd_set \*_Nullable restrict readfds, fd_set \*_Nullable restrict writefds, fd_set \*_Nullable restrict exceptfds, struct timeval \*_Nullable restrict timeout)</x><br>

## Read
<!-- read -->

<lg>pread64</lg><x>(int fd, void buf[.count], size_t count, off_t offset)</x><br>
<lg>preadv</lg><x>(int fd, const struct iovec \*iov, int iovcnt, off_t offset)</x><br>
<lg>preadv2</lg><x>(int fd, const struct iovec \*iov, int iovcnt, off_t offset, int flags)</x><br>
<lg>read</lg><x>(int fd, void buf[.count], size_t count)</x><br>
<lg>readv</lg><x>(int fd, const struct iovec \*iov, int iovcnt)</x><br>

## Write
<!-- write -->

<lg>pwrite64</lg><x>(int fd, const void buf[.count], size_t count, off_t offset)</x><br>
<lg>pwritev</lg><x>(int fd, const struct iovec \*iov, int iovcnt, off_t offset)</x><br>
<lg>pwritev2</lg><x>(int fd, const struct iovec \*iov, int iovcnt, off_t offset, int flags)</x><br>
<lg>write</lg><x>(int fd, const void buf[.count], size_t count)</x><br>
<lg>writev</lg><x>(int fd, const struct iovec \*iov, int iovcn)</x><br>

# Miscellaneous
<!-- 
###############################################################################
##                                miscellaneous                              ##
###############################################################################
-->

<sb>getcpu</sb><x>(unsigned int \*_Nullable cpu, unsigned int \*_Nullable node)</x><br>
getrandom<x>(void buf[.buflen], size_t buflen, unsigned int flags)</x><br>
<lg>landlock_add_rule</lg><x>(int ruleset_fd, enum landlock_rule_type rule_type, const void \*rule_attr, uint32_t flags)</x><br>
landlock_create_ruleset<x>(const struct landlock_ruleset_attr \*attr, size_t size , uint32_t flags)</x><br>
<lg>landlock_restrict_self</lg><x>(int ruleset_fd, uint32_t flags)</x><br>
pkey_alloc<x>(unsigned int flags, unsigned int access_rights)</x><br>
pkey_free<x>(int pkey)</x><br>
pkey_mprotect<x>(void addr[.len], size_t len, int prot, int pkey)</x><br>
rseq<x>(struct rseq \*rseq, uint32_t rseq_len, int flags, uint32_t sig )</x><br>
<sb>sysinfo</sb><x>(struct sysinfo \*info)</x><br>
<sb>uname</sb><x>(struct utsname \*buf)</x><br>