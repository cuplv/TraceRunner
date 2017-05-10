import argparse
import zipfile
import os
import tempfile


# def addToZip(file, zipf):
#     oz = zipfile.ZipFile(zipf,'r')
#
#     filelist = oz.filelist
#
#     z = zipfile.ZipFile(zipf, "w")
#
#
#     for f in filelist:
#         filebytes = oz.read(f.filename)
#         z.write()
#         pass
#
#     z.write(file + "conv.dex", arcname="classes2.dex")
#     z.close()

def updateZip(zipname, filename, datafile):
    # generate a temp file
    tmpfd, tmpname = tempfile.mkstemp(dir=os.path.dirname(zipname))
    os.close(tmpfd)

    # create a temp copy of the archive without filename
    with zipfile.ZipFile(zipname, 'r') as zin:
        with zipfile.ZipFile(tmpname, 'w') as zout:
            zout.comment = zin.comment # preserve the comment
            for item in zin.infolist():
                if item.filename != filename:
                    zout.writestr(item, zin.read(item.filename))

    # replace with the temp archive
    os.remove(zipname)
    os.rename(tmpname, zipname)

    # now add filename with its new data
    with zipfile.ZipFile(zipname, mode='a', compression=zipfile.ZIP_DEFLATED) as zf:
        zf.write(arcname=filename, filename=datafile)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Include external dex file in an existing android apk')
    parser.add_argument('--apk', type=str,
                        help="apk file input",required=True)
    parser.add_argument('--dex', type=str, default="",
                        help="[dex or dir] single dex file to insert into apk")
    parser.add_argument('--dir', type=str, default="",
                        help="[dex or dir] directory to search for dex files")
    args = parser.parse_args()

    if args.dex != "":
        updateZip(args.apk, "classes27.dex",args.dex)
    elif args.dir != "":
        pass
    else:
        raise Exception("set dex file or directory")
