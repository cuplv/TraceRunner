import tracemsg_pb2
import google.protobuf.internal.decoder as decoder

import argparse

def printFromFile(file):
    pbufs = decodeProtobuf(file)
    for pbuf in pbufs:
        print pbuf
def decodeProtobuf(file):
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
        print pbuf
        dataBegin = size + position
        pbufList.append(pbuf)
    return pbufList


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Print human readable data from protobuf trace')
    parser.add_argument('--trace', type=str,
                        help="file with protobuf data encoded by \"writeDelimitedTo\"",required=True)
    args = parser.parse_args()

    printFromFile(args.trace)