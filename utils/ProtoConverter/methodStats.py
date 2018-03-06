import tracemsg_pb2
import google.protobuf.internal.decoder as decoder

import argparse
import tracemsg_pb2
from  google.protobuf.message import DecodeError
from tracemsg_pb2 import TraceMsgContainer

def getCallins(pbufs):
    callins = set()
    for pbuf in pbufs:
        if pbuf.msg.type == TraceMsgContainer.TraceMsg.CALLIN_ENTRY:

            signature = pbuf.msg.callinEntry.method_name
            classname = pbuf.msg.callinEntry.class_name
            callins.add(classname + ":" + signature)
    return callins


def decodeProtobuf(file):
    try:
        f = open(file,'rb')
        data = f.read()
        dataBegin = 0
        pbufList = []
        dataLen = len(data)
        while True:
            if dataBegin >= dataLen:
                return pbufList
            (size, position) = decoder._DecodeVarint(data, dataBegin)
            pbuf = tracemsg_pb2.TraceMsgContainer()
            pbuf.ParseFromString(data[position:position + size])
            dataBegin = size + position
            pbufList.append(pbuf)
        return pbufList
    except DecodeError:
        return None


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='histogram methods in directory of draces by number of referencing traces')
    parser.add_argument('--dir', type=str,
                        help="base directory with traces",required=True)
    parser.add_argument('--type', type=str,
                        help="'cb' or 'ci' or 'cbci' for callbacks, callins, or both respecitvely", required=False)
    args = parser.parse_args()
    if args.type is None:
        import os
        # os.chdir(args.dir)
        hist = {}
        number_processed = 0
        for root, dirs, files in os.walk(args.dir):
            for file in files:
                if file.startswith("trace-"):
                    pbuf = decodeProtobuf(root + "/" + file)
                    number_processed += 1
                    if number_processed % 10 == 0:
                        print number_processed
                    if pbuf is not None:
                        callins = getCallins(pbuf)
                        for callin in callins:
                            if callin in hist:
                                hist[callin] = hist[callin] + 1
                            else:
                                hist[callin] = 1
        for callin in hist:
            print "%i   %s" % ( hist[callin],callin)
        # for file in glob.glob("trace-*"):
        #     traces_to_process.append(file)
        #     pbuf = decodeProtobuf(file)
        #     getCallins(pbuf)


