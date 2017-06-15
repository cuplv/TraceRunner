from multiprocessing import Process
import time

def f(name):
    time.sleep(5)
    print name

if __name__ == '__main__':
    p = Process(target=f, args=("argument",))
    p.start()
    time.sleep(4)
    if p.is_alive():
        print "terminating p"
        p.terminate()
    p.join()
    print "done"
