## Installing dependencies

---

## Testing

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