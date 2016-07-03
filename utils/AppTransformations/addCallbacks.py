__author__ = 's'
import sys
import os
import optparse
import re


def convertLine(line):

    sline = line.split(".")

    doNoDotSubs = True

    for isline in sline:
        if isline.startswith("findViewById"):
            doNoDotSubs = False
    if doNoDotSubs:
        line = re.sub(r"(?!\.)(findViewById\([.a-zA-Z0-9]+\))", r"TraceRunnerCallbackRegistration.registerForView(\1)",
                  line)
    line = re.sub(r"([a-zA-Z][a-zA-Z0-9_.]*)\.(findViewById\([.a-zA-Z0-9_]+\))",
                  r"TraceRunnerCallbackRegistration.registerForView(\1.\2)", line)

    return line


def convert_java(fname, package):
    f = open(fname,'r')
    lines = f.readlines()


    modlines = []
    for i in xrange(len(lines)):
        line = lines[i]
        tline = line.strip()
        if tline.startswith("."):
            if len(modlines) == 0:
                raise Exception("first line begins with dot, not a valid java file")
            appline = modlines[-1].strip() + tline

            modlines = modlines[:-1]
            modlines.append(convertLine(appline))
            pass
        else:
            modlines.append(convertLine(line))
    f.close()

    # clear and write modified data
    f = open(fname, 'w+')
    f.seek(0)
    f.truncate()
    #f.write("import " + package + ".TraceRunnerCallbackRegistration;")
    index = 0
    for line in modlines:
        if index == 2:
            f.write("import " + package + ".TraceRunnerCallbackRegistration;")
        f.write(line)
        index+=1
    f.close()

    # #run sed command on file
    # call(["sed", "-i.bak", "s:\([^\.]\)\(findViewById([.a-zA-Z0-9]*)\):\1(registerForView(\2)):", fname])
    # call(["sed", "-i.bak", "s:\([^\s]\)\([a-zA-Z][\.a-zA-Z0-9\_]*\)\(\.findViewById([.a-zA-Z0-9]*)\):\1(registerForView(\2\3)):",
    #        fname])




def convert_app(directory):
    for root, dirs, files in os.walk(directory):
        for f in files:
            if f.endswith(".java"):
                convert_java(f)

def main():
    p = optparse.OptionParser()
    p.add_option('-d', '--directory', help="apply to all files in a directory")
    p.add_option('-f', '--file', help="apply to single java file")
    p.add_option('-p', '--package', help="package of the app being traced")


    def usage(msg=""):
        if msg: print "----%s----\n" % msg
        p.print_help()
        sys.exit(1)

    opts, args = p.parse_args()

    if not opts.package:
        usage("package required")

    if opts.file:
        file = opts.file
        convert_java(file, opts.package)

    if opts.directory:
        dir = opts.directory
        pass

    pass
if __name__ == '__main__':
    main()