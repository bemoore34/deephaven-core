"""
script.py

Python script to run the validate demo scripts action. This script reads from every markdown file in the
given directory, finds the code within the ```python ``` tags, and runs it in Deephaven using the pyclient package.
"""
from pydeephaven import Session, DHError

import sys
import time
import os

PYTHON_START_TAG = "```python"
PYTHON_END_TAG = "```"

def extract_python_scripts(file_path):
    """
    Extracts the python scripts between the Python tags in the file at the given path, and
    returns them combined as a single string

    Parameters:
        file_path (str): The path to the file

    Returns:
        str: The combined string of Python scripts in the file
    """
    python_scripts = []
    with open(file_path) as f:
        in_python_script = False
        current_script = None
        for line in f.readlines():
            if (PYTHON_START_TAG in line) and (not in_python_script):
                in_python_script = True
                current_script = ""
            elif (PYTHON_END_TAG in line) and in_python_script:
                in_python_script = False
                python_scripts.append(current_script)
            elif in_python_script:
                current_script += line

    return "\n".join(python_scripts)

def main(demo_scripts_path: str, host: str, port: int, max_retries: int):
    """
    Main method for the script. Reads each file line by line and grabs lines
    between the ```python ``` tags to run in Deephaven.

    Parameters:
        demo_scripts_path (str): The path to the demo notebooks
        host (str): The host name of the Deephaven instance
        port (int): The port on the host to access
        max_retries (int): The maximum attempts to retry connecting to Deephaven

    Returns:
        None
    """

    print(f"Attempting to connect to host at {host} on port {port}")

    #Simple retry loop in case the server tries to launch before Deephaven is ready
    count = 0
    session = None
    while (count < max_retries):
        try:
            session = Session(host=host, port=port)
            print("Connected to Deephaven")
            break
        except DHError as e:
            print("Failed to connect to Deephaven... Waiting to try again")
            print(e)
            time.sleep(5)
            count += 1
        except Exception as e:
            print("Unknown error when connecting to Deephaven... Waiting to try again")
            print(e)
            time.sleep(5)
            count += 1
    if session is None:
        sys.exit(f"Failed to connect to Deephaven after {max_retries} attempts")

    is_error = False
    error_files = []
    for file_path in os.popen(f"find {demo_scripts_path} -name '*.md' | sort").read().split("\n"):
        if len(file_path) > 0:
            print(f"Reading file {file_path}")
            script_string = extract_python_scripts(file_path)
            try:
                session.run_script(script_string)
                time.sleep(5)
            except DHError as e:
                print(e)
                print(f"Deephaven error when trying to run code in {file_path}")
                error_files.append(file_path)
                is_error = True
            except Exception as e:
                print(e)
                print(f"Unexpected error when trying to run code in {file_path}")
                error_files.append(file_path)
                is_error = True
    if is_error:
        error_files_print = "\n".join(error_files)
        print(f"Errors were found in the following notebooks:\n {error_files_print}")
        sys.exit("At least 1 demo notebook failed to run. Check the logs for information on what failed")

usage = """
usage: python script.py demo_scripts_path host port max_retries
"""

if __name__ == '__main__':
    if len(sys.argv) > 5:
        sys.exit(usage)

    try:
        demo_scripts_path = sys.argv[1]
        host = sys.argv[2]
        port = int(sys.argv[3])
        max_retries = int(sys.argv[4])
    except:
        sys.exit(usage)

    main(demo_scripts_path, host, port, max_retries)
