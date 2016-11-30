import vk
import json
import os
import time
import sys


REQUEST_FIELDS = ['country', 'city', 'universities', 'education', 'occupation', 'career']
FIELDS_TO_REMOVE = ['uid', 'first_name', 'last_name', 'hidden']
NECESSARY_FIELDS = ['career', 'universities', 'education', 'occupation',
                    'university_name', 'university', 'faculty_name', 'faculty']

queue = []

api = vk.API(vk.Session())
processed_friend_ids = set()


def fetch_all_friends_data(node_id, fields):
    offset = 0
    count = 50
    res = []
    current_response = []
    while len(current_response) == count or offset == 0:
        try:
            current_response = api.friends.get(user_id=node_id, count=count, offset=offset, fields=fields)
        except Exception:
            current_response = []
        time.sleep(0.4)
        res += current_response
        offset += count

    return res


def purge_user_data(user_data):
    res = []
    for item in user_data:
        for necessary_field in NECESSARY_FIELDS:
            if necessary_field in item and \
                            item[necessary_field] != 0 and \
                            item[necessary_field] != '' and \
                            item[necessary_field] != []:
                res.append(item)
                break

    return res


def trim_user_data(user_data):
    for item in user_data:
        for field_to_remove in FIELDS_TO_REMOVE:
            if field_to_remove in item:
                del item[field_to_remove]

    return user_data


def process_all_friends_data(node_id):
    global processed_friend_ids, REQUEST_FIELDS, OUTPUT_FILE
    friends = fetch_all_friends_data(node_id, REQUEST_FIELDS)
    friends = [item for item in friends if item['user_id'] not in processed_friend_ids]
    ids = [item['user_id'] for item in friends]
    for cur_id in ids:
        processed_friend_ids.add(cur_id)
    friends = purge_user_data(friends)
    friends = trim_user_data(friends)
    university_data_file = open(OUTPUT_FILE, 'a')
    for item in friends:
        university_data_file.write(json.dumps(item))
        university_data_file.write('\n')
    university_data_file.close()

    return ids


def crawl_graph(start_node_id, process_data_and_get_ids_fn):
    global queue, TIME_LIMIT, DATASET_SIZE

    start_time = time.time()

    if len(queue) == 0:
        queue = [(start_node_id, 0)]
    while len(queue) > 0:
        node_id, cur_depth = queue[0]
        print >> sys.stderr, 'started processing of', node_id, 'on depth', cur_depth
        queue = queue[1:]
        new_node_ids = process_data_and_get_ids_fn(node_id)
        print >> sys.stderr, 'new ids count:', len(new_node_ids), 'total ids count:', len(processed_friend_ids)
        queue += zip(new_node_ids, [cur_depth + 1 for _ in range(len(new_node_ids))])
        persist_execution_state()
        print >> sys.stderr, 'time already passed:', time.time() - start_time, 'time limitation:', TIME_LIMIT
        if TIME_LIMIT is not None and time.time() - start_time > TIME_LIMIT:
            print >> sys.stderr, 'time limitation exceeded'
            break
        print >> sys.stderr, 'data already ingested:', len(processed_friend_ids), 'data size limitation:', DATASET_SIZE
        if DATASET_SIZE is not None and len(processed_friend_ids) > DATASET_SIZE:
            print >> sys.stderr, 'data size exceeded'
            break


def persist_execution_state():
    global processed_friend_ids, queue
    execution_state_file = open(EXECUTION_STATE_FILE, 'w')
    execution_state = [list(processed_friend_ids), queue]
    execution_state_file.write(json.dumps(execution_state))
    execution_state_file.close()


def load_execution_state():
    global processed_friend_ids, queue
    try:
        execution_state_file = open(EXECUTION_STATE_FILE, 'r')
        execution_state = json.load(execution_state_file)
        processed_friend_ids, queue = execution_state
        processed_friend_ids = set(processed_friend_ids)
        execution_state_file.close()
    except IOError:
        print >> sys.stderr, 'execution state does not exist'


START_NODE = int(os.getenv('VK_INGEST_START_NODE', '11582866')) # Vlad as default

EXECUTION_STATE_FILE = os.getenv('VK_INGEST_EXECUTION_STATE_FILE', '/data/ingest-vk-data.state')
OUTPUT_FILE = os.getenv('VK_INGEST_OUTPUT_FILE', '/data/vk.data')

TIME_LIMIT = int(os.environ.get('VK_INGEST_TIME_LIMIT'))
DATASET_SIZE = int(os.environ.get('VK_INGEST_DATASET_SIZE'))

print >> sys.stderr, 'start node:', START_NODE
print >> sys.stderr, 'path to execution state file:', EXECUTION_STATE_FILE
print >> sys.stderr, 'path to output file:', OUTPUT_FILE
print >> sys.stderr, 'time limit:', TIME_LIMIT
print >> sys.stderr, 'data size limit:', DATASET_SIZE

load_execution_state()
crawl_graph(START_NODE, process_all_friends_data)
