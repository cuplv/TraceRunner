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
    print "decoding"
    trace = protoPrinter.decodeProtobuf(args.trace)[0]
    print "truncating"
    newtrace = truncate_corrupted(trace)
    print len(newtrace)
    outfile = open(args.out,'w')
    write_proto(newtrace,outfile)
    outfile.close()