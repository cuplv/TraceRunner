# protoc -I=$SRC_DIR --python_out=$DST_DIR $SRC_DIR/addressbook.proto
import os
import subprocess

def compileProto():
    dirname = os.path.dirname(__file__)

    split = dirname.split("/")
    projRoot = "/".join(split[0:-2])
    srcDir = projRoot + "/TraceRunnerRuntimeInstrumentation/tracerunnerinstrumentation/src/main/proto/edu/colorado/plv/tracerunner_runtime_instrumentation/"
    protoFile = "tracemsg.proto"

    subprocess.call(["protoc", "-I=" + srcDir, "--python_out="+ dirname, srcDir+protoFile])

if __name__ == "__main__":
    compileProto()