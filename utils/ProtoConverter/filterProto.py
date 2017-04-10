import protoPrinter
import argparse
import os
import tracemsg_pb2


# returns list of (trace, msgCount)
def filter_files(tracelist, clazz, method):
    traces_with_method = set()
    for tracefile in tracelist:
        current_trace = protoPrinter.decodeProtobuf(tracefile)
        has_method = 0
        for msg in current_trace[0]:
            if msg.msg.type == tracemsg_pb2.TraceMsgContainer.TraceMsg.CALLIN_ENTRY:
                className = msg.msg.callinEntry.class_name
                if className == clazz:
                    methodName = msg.msg.callinEntry.method_name
                    if methodName == method:
                        has_method += 1
        if has_method > 0:
            traces_with_method.add((tracefile,has_method))
    return traces_with_method


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="filter list of proto files for ones which contain given callin")
    parser.add_argument('--tracelist', help="list of trace files to filter", required=True)
    parser.add_argument('--filter_class', help='class of method', required=True)
    parser.add_argument('--filter_method', help='method signature, can be : separated list', required=True)
    parser.add_argument('--basedir', help='directory that all of tracelist is relative to')
    args = parser.parse_args()
    f = open(args.tracelist, 'r')
    files = f.readlines()
    if args.basedir is not None:
        fullfiles = [os.path.join(args.basedir, f).strip() for f in files]
        files = fullfiles

    traces_with_method = set()
    for filter_method in args.filter_method.split(":"):
        traces_with_method_single = filter_files(files, args.filter_class, filter_method)
        traces_with_method = traces_with_method.union(traces_with_method_single)
    for trace in traces_with_method:
        print trace[0] + "\t" + str(trace[1])
