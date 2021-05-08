## Installing dependencies

### General

The tool for installing dependencies `install-deps.py` will install all tools and prepare an environment for further
testing/developing.

### Arguments

```commandline
python install-deps.py
python install-deps.py verbosity_level
```

The tool receives non or one argument, if no arguments have been passed the default verbosity level will be set to 0, if
the argument has been passed then it should be in the format *v* or *vv* (verbosity level 1 or 2). <br/>
Verbosity levels:

- Level 0 - only mandatory information will be printed.
- Level 1 - level 0 plus error and warning logs.
- Level 2 - level 1 plus full output log.

---

## Testing

### General

The tool for running tests `run-test.py` will create multi-user, multi-client (multiple clients behind the same
username) an environment with different loads based on test configuration for system stress testing.

### Arguments

```commandline
python run-test.py test_config_path
python run-test.py verbosity_level test_config_path 
```

The tool receives one or two-argument, if one argument has been passed the default verbosity level will be set to 0, if
the arguments have been passed then the first one should be in the format *v* or *vv* (verbosity level 1 or 2). <br/>
Verbosity levels:

- Level 0 - only mandatory information will be printed.
- Level 1 - level 0 plus error and warning logs.
- Level 2 - level 1 plus full output log.

The second argument of the tool is a path for the test configuration. A detailed explanation of the configuration
structure is provided bellow (value inside [ ] represents JSON data types):

```json
{
  "test": "[STRING] Test name?",
  "entry_point": "[STRING] Load balancer address?",
  "managers": [
    {
      "address": "[STRING] 1th lambda manager address?",
      "config_path": "[STRING] 1th lambda manager configuration path?"
    }
  ],
  "users": [
    {
      "username": "[STRING] 1th username?",
      "commands": [
        {
          "command": "remove",
          "lambda_name": "[STRING] Function name?"
        },
        {
          "command": "upload",
          "allocate": "[INTEGER] How many instances the user wants allocate for this function?",
          "lambda_name": "[STRING] Function name?",
          "source": "[STRING] Function source code path?"
        },
        {
          "command": "send",
          "sending_info": [
            {
              "iterations": "[INTEGER] How many test iterations...",
              "num_requests": "[INTEGER] with how many request per iteration...",
              "num_clients": "[INTEGER] with how many clients in parallel?",
              "lambda_name": "[STRING] Function name?",
              "args_pool": [
                "[ANY] arg1",
                "[ANY] arg2",
                "[ANY] arg3"
              ],
              "output": "[STRING] Where the ApacheBench should store results?"
            }
          ]
        },
        {
          "command": "pause",
          "duration": "[INTEGER] How many seconds to wait?"
        }
      ]
    }
  ]
}
```

---

## Plotting

### General

The tool for generating plots `run-plot.py` will output plots based on plot configuration.

### Arguments

```commandline
python run-plot.py test_config_path
python run-plot.py verbosity_level test_config_path 
```

The tool receives one or two-argument, if one argument has been passed the default verbosity level will be set to 0, if
the arguments have been passed then the first one should be in the format *v* or *vv* (verbosity level 1 or 2). <br/>
Verbosity levels:

- Level 0 - only mandatory information will be printed.
- Level 1 - level 0 plus error and warning logs.
- Level 2 - level 1 plus full output log.

The second argument of the tool is a path for the plot configuration. A detailed explanation of the configuration
structure is provided bellow (value inside [ ] represents JSON data types):

```json
{
  "plot": "[STRING] Plotting name?",
  "start_server": {
    "port": "[INTEGER] Server port?",
    "img_width": "[INTEGER] Image width?",
    "img_height": "[INTEGER] Image height?"
  },
  "plots": [
    {
      "type": "latency",
      "output": "[STRING] Where to store plot image?",
      "show_image": "[BOOL] Show image after it's generated?",
      "data": [
        {
          "title": "[STRING] 1th title?",
          "filepath": "[STRING] Path to 1th input datafile?",
          "regex": "[STRING] Regex for above datafile?"
        },
        {
          "title": "[STRING] 2th title?",
          "filepath": "[STRING] Path to 2th input datafile?",
          "regex": "[STRING] Regex for above datafile?"
        },
        {
          "title": "[STRING] 3th title?",
          "filepath": "[STRING] Path to 3th input datafile?",
          "regex": "[STRING] Regex for above datafile?"
        }
      ]
    },
    {
      "type": "throughput",
      "output": "[STRING] Where to store plot image?",
      "show_image": "[BOOL] Show image after it's generated?",
      "data": [
        {
          "title": "[STRING] 1th title?",
          "filepath": "[STRING] Path to 1th input datafile?",
          "regex": "[STRING] Regex for above datafile?"
        },
        {
          "title": "[STRING] 2th title?",
          "filepath": "[STRING] Path to 2th input datafile?",
          "regex": "[STRING] Regex for above datafile?"
        },
        {
          "title": "[STRING] 3th title?",
          "filepath": "[STRING] Path to 3th input datafile?",
          "regex": "[STRING] Regex for above datafile?"
        }
      ]
    },
    {
      "type": "footprint",
      "output": "[STRING] Where to store plot image?",
      "show_image": "[BOOL] Show image after it's generated?",
      "data": [
        {
          "title": "Lambda footprint",
          "regex": "Maximum resident set size \\(kbytes\\):\\s*(\\d*)"
        }
      ]
    },
    {
      "type": "startup",
      "output": "[STRING] Where to store plot image?",
      "show_image": "[BOOL] Show image after it's generated?",
      "data": [
        {
          "title": "Lambda startup",
          "regex": "[STRING] Regex for startup?"
        }
      ]
    },
    {
      "type": "scalability",
      "output": "[STRING] Where to store plot image?",
      "show_image": "[BOOL] Show image after it's generated?",
      "data": [
        {
          "title": "System scalability"
        }
      ]
    },
    {
      "type": "total memory",
      "output": "[STRING] Where to store plot image?",
      "show_image": "[BOOL] Show image after it's generated?",
      "data": [
        {
          "title": "Total system memory",
          "regex": "[STRING] Regex for total system memory?"
        }
      ]
    }
  ]
}
```