import protoPrinter
import argparse
import os
import tracemsg_pb2



def get_set_of_methods(trace):
    methods = set()
    for msg in trace:
        if msg.msg.type == tracemsg_pb2.TraceMsgContainer.TraceMsg.CALLIN_ENTRY:
            methods.add(msg.msg.callinEntry.class_name + " ; " + msg.msg.callinEntry.method_name)
    return methods
def filter_files(tracelist, clazz, method):
    traces_with_method = set()
    length = len(tracelist)
    cur = 0
    if cur % 10 == 0:
        print str(cur) + "/" + str(length)
    cur += 1
    for tracefile in tracelist:
        has_method = trace_contains_method(clazz, method, tracefile)
        if has_method > 0:
            traces_with_method.add((tracefile,has_method))
    return traces_with_method


def trace_contains_method(clazz, method, tracefile, count=False):
    current_trace = protoPrinter.decodeProtobuf(tracefile)
    has_method = 0
    for msg in current_trace[0]:
        if msg.msg.type == tracemsg_pb2.TraceMsgContainer.TraceMsg.CALLIN_ENTRY:
            className = msg.msg.callinEntry.class_name
            if className == clazz:
                methodName = msg.msg.callinEntry.method_name
                if methodName == method:
                    has_method += 1
                    if not count:
                        break
    return has_method

def write_mf(method_frequency, stream):
    method_frequency_tup = [(method_frequency[m], m) for m in method_frequency]
    method_frequency_tup.sort(key=lambda x: x[0])
    for tup in method_frequency_tup:
        f.write(str(tup[0]) + " , " + tup[1] + "\r")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="filter list of proto files for ones which contain given callin")
    parser.add_argument('--trace_list', help="list of trace files to filter", required=False)
    parser.add_argument('--filter_class', help='class of method', required=False)
    parser.add_argument('--filter_method', help='method signature, can be : separated list', required=False)
    parser.add_argument('--basedir', help='directory that all of tracelist is relative to')
    parser.add_argument('--trace', help="single trace to check, prints trace path if found")
    parser.add_argument('--callin_histogram', help="give list of traces and optional base directory and get a frequency count of how many traces mention a given method")
    args = parser.parse_args()


    fullfiles = None
    if args.trace_list is not None:
        f = open(args.trace_list, 'r')
        files = f.readlines()
        if args.basedir is not None:
            fullfiles = [os.path.join(args.basedir, f.strip()).strip() for f in files]
            files = fullfiles
    if args.filter_class is not None and args.filter_method is not None:
        if args.trace_list is not None:
            traces_with_method = set()
            for filter_method in args.filter_method.split(":"):
                traces_with_method_single = filter_files(files, args.filter_class, filter_method)
                traces_with_method = traces_with_method.union(traces_with_method_single)
            for trace in traces_with_method:
                print trace[0] + "\t" + str(trace[1])
        elif args.trace is not None:
            inst = 0
            for filter_method in args.filter_method.split(":"):
                inst += trace_contains_method(args.filter_class, filter_method, args.trace)
                if inst > 0:
                    break
            if inst > 0:
                print args.trace
    if args.callin_histogram is not None:
        method_frequency = {}
        count = 0
        fullfileslen = len(fullfiles)
        for file in fullfiles:
            if count%10 == 0:
                print str(count) + "/" + str(fullfileslen)
            if count %100 == 0:
                f = open("protohist_lim" + str(count),'w')
                write_mf(method_frequency,f)
                f.close()
            count += 1
            current_trace = protoPrinter.decodeProtobuf(file)[0]
            methods = get_set_of_methods(current_trace)
            for method in methods:
                if method in method_frequency:
                    method_frequency[method] = method_frequency[method] + 1
                else:
                    method_frequency[method] = 1


