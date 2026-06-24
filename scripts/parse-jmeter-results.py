#!/usr/bin/env python3

import json
import sys
import os
from pathlib import Path

def parse_jmeter_results(results_dir):
    """Parse JMeter results and check thresholds"""
    report_file = Path(results_dir) / "full-report" / "statistics.json"

    if not report_file.exists():
        print("No statistics.json found")
        return True

    with open(report_file) as f:
        data = json.load(f)

    # Check error rate
    total_requests = 0
    failed_requests = 0

    for stat in data:
        total_requests += stat.get('total', 0)
        failed_requests += stat.get('failureCount', 0)

    if total_requests > 0:
        error_rate = (failed_requests / total_requests) * 100
        print(f"Error rate: {error_rate:.2f}%")

        if error_rate > 1.0:
            print(f"❌ Error rate {error_rate:.2f}% exceeds threshold of 1%")
            return False

        print("✅ Error rate is acceptable")

    # Check response time
    avg_response_time = 0
    count = 0

    for stat in data:
        if 'meanResTime' in stat:
            avg_response_time += stat['meanResTime']
            count += 1

    if count > 0:
        avg_response_time /= count
        print(f"Average response time: {avg_response_time:.2f}ms")

        if avg_response_time > 200:
            print(f"❌ Average response time {avg_response_time:.2f}ms exceeds threshold of 200ms")
            return False

        print("✅ Response time is acceptable")

    return True

if __name__ == "__main__":
    results_dir = sys.argv[1] if len(sys.argv) > 1 else "results/"
    success = parse_jmeter_results(results_dir)
    sys.exit(0 if success else 1)