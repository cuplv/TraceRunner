Add Network
===========
file: AppTransformations/addNetwork.py
base usage: python addNetwork.py --apk [apk file to add network to] --output [location of output]
purpose: This script takes an apk file and adds the network permission.  This is useful since TraceRunner will throw an IOException if the app does not have this.


Print Proto file
===========
file: ProtoConverter/protoPrinter.py
base usage: python protoPrinter.py --trace [trace to print]
purpose: prints all human readable information from trace file
requirements: generate protobuf code for python

protoDataDependency.py
===========
(Not implemented yet)
purpose: print a dot data dependency graph of callins and callbacks from a trace.  Optionally filter for relevance to a callback.
requirements: generate protobuf code for python

Generate Protobuf Code For Python
===========
file: ProtoConverter/genPythonProto.py
base usage: python proto
description: this generates tracemsg_pb2.py (tracemsg_pb2.pyc is automatically generated py python) which are needed for python scripts that directly read protobuf files.
