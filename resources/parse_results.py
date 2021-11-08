import json
import numpy as np
import sys


def _error(actual, predicted):
    """ Simple error """
    return actual - predicted


def _percentage_error(actual, predicted):
    """
    Percentage error
    Note: result is NOT multiplied by 100
    """
    return _error(actual, predicted) / actual


def parse_results(input_filename, output_filename):
    with open(input_filename) as f:
        with open(output_filename, 'w') as fw:
            fw.write(
                "query\tactualResult\tpercentage_error\tgraphSize\tmaxK\tscale\tepsilon\tSensitivity\telasticStability\ttripleSelectivity\tmapMostFreqValue\n")
            for line in f.readlines():
                query_results = json.loads(line)
                private_result = query_results['privateResult']
                actual_result = np.array(query_results['result'])
                if (actual_result[1] != 0):
                    percentage_error = _percentage_error(actual_result, private_result)
                else:
                    percentage_error = 10000
                # print("actual {} error {}".format(actual_result, abs(np.mean(percentage_error)*100)))
                fw.write(str(query_results['query']) + str('\t'))
                fw.write(str(actual_result[1]) + '\t')
                fw.write(str(abs(np.mean(percentage_error * 100))) + str('\t'))
                fw.write(str(query_results['graphSize']) + str('\t'))
                fw.write(str(query_results['maxK']) + str('\t'))
                fw.write(str(query_results['scale']) + str('\t'))
                fw.write(str(query_results['epsilon']) + str('\t'))
                fw.write(str(query_results['sensitivity']) + str('\t'))
                fw.write(str(query_results['elasticStability']) + str('\t'))
                fw.write(str(query_results['mapMostFreqValue']))
                # fw.write(str(query_results['mapMostFreqValueStar']))
                fw.write('\n')


if __name__ == '__main__':
    if len(sys.argv) == 1:
        print('Usage: python parse_privacy_results.py JSON result_file output_file ')
    else:
        print(sys.argv[1], sys.argv[2])
        parse_results(sys.argv[1], sys.argv[2])
