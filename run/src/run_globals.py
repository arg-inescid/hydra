import os
import sys

# Global settings.
MAX_VERBOSITY_LVL = 2

# Lambda manager global variables.
LAMBDA_MANAGER_DIR = os.path.join(os.path.dirname(sys.argv[0]), "..", "..", "core", "lambda-manager")
LAMBDA_MANAGER_LOGS_DIR = os.path.join(LAMBDA_MANAGER_DIR, "manager_logs")
LAMBDA_LOGS_DIR = os.path.join(LAMBDA_MANAGER_DIR, "lambda_logs")

LAMBDA_MANAGER_LOG_FILE = os.path.join(LAMBDA_MANAGER_LOGS_DIR, "lambda_manager.log")
SETUP_DB_VM_LOC = os.path.join(LAMBDA_MANAGER_DIR, "src", "scripts", "qemu-jvm")
SETUP_DB_VM_FILE = "setup_debian_vm.sh"

# Proxy global variables.
PROXY_DIR = os.path.join(LAMBDA_MANAGER_DIR, "..", "lambda-proxies")

# Testing global variables.
CONFIG_DIR = os.path.join(os.path.dirname(sys.argv[0]), "..", "configs")
TEST_CONFIG_DIR = os.path.join(CONFIG_DIR, "tests")
MANAGER_CONFIG_DIR = os.path.join(CONFIG_DIR, "manager")

MANAGER_CONFIG_PATH = os.path.join(MANAGER_CONFIG_DIR, "default-manager.json")
BENCHMARK_BUILD_SCRIPT = "build_script.sh"

# Web UI global variables.
WEB_UI_DIR = os.path.join(os.path.dirname(sys.argv[0]), "..", "..", "web-ui")
