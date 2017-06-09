import argparse

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Print human readable data from protobuf trace')
    parser.add_argument('--trace', type=str,
                        help="file with protobuf data encoded by \"writeDelimitedTo\"", required=True)
    parser.add_argument('--out', type=str, help= "output of trace")
    parser.add_argument('--app_name', help="name of the app")
    #     optional string app_site_of_origin = 2; //github, bitbucket, appstore etc
    parser.add_argument('--app_site_of_origin', help="where the app was downloaded, github, bitbucket, appstore" +
                                                     "use base url such as http://www.github.com")
    # optional string app_commit_hash = 3;
    parser.add_argument('--app_commit_hash', help="git commit hash of the app")
    # optional string apk_name = 4;
    parser.add_argument('--apk_name', help="name of the apk used for tracoing (some builds make multiple " +
                                            "apk files, use path to root of project")
    # optional string apk_sha1_hash = 5;
    parser.add_argument('--apk_sha1_hash', help="if the apk was downloaded from an app store put the sha1 here")
    # optional string app_vcs_organization = 6;
    parser.add_argument('--app_vcs_organization', help="organization or username on github or bitbucket")
    # optional string app_vcs_reponame = 7;
    parser.add_argument('--app_vcs_reponame', help="repositor")
    # optional string trace_runner_commit = 8;
    # optional string trace_runner_additional_info = 9;
    args = parser.parse_args()
