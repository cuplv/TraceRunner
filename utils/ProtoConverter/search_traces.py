import argparse
import protoPrinter
import os
import google
import tracemsg_pb2

def ends_in_error(trace):
    length = len(trace)
    last = trace[length-1]
    exception = tracemsg_pb2.TraceMsgContainer.TraceMsg.MsgType.CALLBACK_EXCEPTION
    print last


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Print human readable data from protobuf trace')
    parser.add_argument('--folder', type=str,
                        help="folder to search",required=True)
    parser.add_argument('--sig_contains', type=str,
                        help="substring contained in method signature")
    parser.add_argument('--class', type = str,
                        help="substring contained in method signature")
    args = parser.parse_args()

    truncated = 0
    end_in_error = 0
    well_formed = 0 #traces which don't have either of the above
    lengths = [] #list of all sizes for good traces
    for root,dirnames, filenames in os.walk(args.folder):
        for file in filenames:

            try:
                fullFile = os.path.join(root,file)
                protodecode = protoPrinter.decodeProtobuf(fullFile)
                length = len(protodecode)
                print fullFile + ":" + str(length)
                if(ends_in_error(protodecode)):
                    end_in_error+=1
                    print "    ends in error"
                else:
                    well_formed +=1
                    print "    well formed"
            except google.protobuf.message.DecodeError:
                print fullFile + ":trunc"
                truncated+=1
