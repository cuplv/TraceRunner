import argparse
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Take uninstrumented APK and create traces')
    parser.add_argument('--path', type=str, help="full path of apk")
    parser.add_argument('--number', type=int, help="number of traces")
    traceRunnerDir =  
