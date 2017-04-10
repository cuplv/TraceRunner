import tracemsg_pb2
import google.protobuf.internal.decoder as decoder

import argparse

def printFromFile(file):
    pbufs, err = decodeProtobuf(file)
    for pbuf in pbufs:
        print pbuf

def checkFromFile(file, print_p):
    pbufs,err = decodeProtobuf(file)
    last = 0
    for pbuf in pbufs:
        if pbuf.msg.message_id == last:
            last += 1
            if print_p:
                print pbuf
        else:
            if print_p:
                print pbuf
            print "---missing: " + str(pbuf.msg.message_id - last)
            break
def decodeProtobuf(file):
    f = open(file,'rb')
    data = f.read()
    dataBegin = 0
    pbufList = []
    dataLen = len(data)
    error = None
    while True:
        if dataBegin >= dataLen:
            return pbufList,None
        (size, position) = decoder._DecodeVarint(data, dataBegin)
	try:
        	pbuf = tracemsg_pb2.TraceMsgContainer()
        	pbuf.ParseFromString(data[position:position + size])
        	dataBegin = size + position
        	pbufList.append(pbuf)
	except Exception as e:
		error = e
		break
    return pbufList,error


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Print human readable data from protobuf trace')
    parser.add_argument('--trace', type=str,
                        help="file with protobuf data encoded by \"writeDelimitedTo\"",required=True)
    parser.add_argument('--check', help="check for missing messages")
    args = parser.parse_args()

    if not args.check:
    	printFromFile(args.trace)
    else:
        checkFromFile(args.trace,False)
