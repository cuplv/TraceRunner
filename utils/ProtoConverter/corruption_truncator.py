import tracemsg_pb2
import google.protobuf.internal.decoder as decoder
import argparse
import protoPrinter
from google.protobuf.internal import encoder

def truncate_corrupted(trace):
    last = 0;
    newtrace = []
    total = len(trace)
    for message in trace:
        if message.msg.message_id != last:
            break
        last += 1
        # if last%100 == 0:
        #     print "(%i,%i)" % (last,total)
        newtrace.append(message)

    return newtrace
def sort_trace(trace):
    return sorted(trace, key=lambda m: m.msg.message_id)

def deduplicate_exceptions(trace):
    newtrace = []
    for message in trace:
        if message.msg.type == message.msg.CALLIN_EXEPION:  # tracemsg_pb2.TraceMsgContainer.TraceMsg.MsgType.CALLIN_EXEPION:
            last = newtrace[-1]
            if last.msg.type == last.msg.CALLIN_EXEPION:
                msg_exception = message.msg.callinException
                prev_exception = last.msg.callinException
                if msg_exception.throwing_class_name != prev_exception.throwing_class_name \
                        or msg_exception.throwing_method_name != msg_exception.throwing_method_name:
                    raise Exception("Two different callin exceptions adjascent to one another")
                else:
                    print ""
            else:
                newtrace.append(message)
        else:
            newtrace.append(message)
    return newtrace



def write_proto( msgs,buff):
    out = None
    for msg in msgs:
        serializedMessage = msg.SerializeToString()
        delimiter = encoder._VarintBytes(len(serializedMessage))
        if out is None:
            out = delimiter + serializedMessage
        else:
            out = out + delimiter + serializedMessage
    buff.write(out)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Chop off trace at corruption based on message indices')
    parser.add_argument('--trace', type=str,
                        help="trace file",required=True)
    parser.add_argument('--out', help="write sub trace here")
    args = parser.parse_args()
    trace = protoPrinter.decodeProtobuf(args.trace)[0]
    trace_sorted = sort_trace(trace)
    newtrace = truncate_corrupted(trace_sorted)
    print args.trace + ", old trace len: " + str(len(trace)) + "new trace len: " + str(len(newtrace))
    outfile = open(args.out,'w')
    newnewtrace = deduplicate_exceptions(newtrace)
    write_proto(newnewtrace,outfile)
    outfile.close()