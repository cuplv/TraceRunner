
from redisJobQueue.ops import load_search_data

queues = {}

queues['a-content-jobs'] = ['android.content.res.TypedArray_recycle_typedarray_buildable.json']
queues['a-graphics-jobs'] = ['android.graphics.Bitmap_recycle_bitmap_buildable.json']
queues['asynctask-jobs'] = ['AsyncTask_executeOnExecutor_search_buildable.json','AsyncTask_execute_search_buildable.json']
queues['frag-act-jobs'] = ['Fragment_startActivityForResult_search_buildable.json','Fragment_startActivity_search_buildable.json']
queues['frag-get-jobs'] = ['Fragment_getString_search_buildable.json','Fragment_getLoaderManager_search_buildable.json','Fragment_getResources_search_buildable.json',
'Fragment_getText_search_buildable.json']
queues['frag-trans-jobs'] = ['FragmentTransaction_commit_search_buildable.json']
queues['frag-rest-jobs'] = ['Fragment_instantiateChildFragmentManager_search_buildable.json','Fragment_requestPermission_search_buildable.json'
                           ,'Fragment_setRetainInstance_search_buildable.json']
queues['media-jobs'] = ['MediaPlayer_pause_search_buildable.json','MediaPlayer_prepareAsync_search_buildable.json'
                       ,'MediaPlayer_start_search_buildable.json','MediaPlayer_stop_search_buildable.json']
queues['view-jobs'] = ['View_buildLayer_search_buildable.json']

queues2 = {}

queues2['alert-dialog-jobs'] = ['android.app.AlertDialog_dismiss_dialog_buildable.json','android.app.AlertDialog_show_dialog_buildable.json']
queues2['other-dialog-jobs'] = ['android.app.Dialog_dismiss_dialog_buildable.json','android.app.Dialog_show_dialog_buildable.json',
                                'android.app.Presentation_show_dialog_buildable.json','android.app.ProgressDialog_show_dialog_buildable.json']

test = {}
test['test-jobs'] = ['Fragment_startActivity_search_buildable.json','Fragment_getString_search_buildable.json','MediaPlayer_pause_search_buildable.json','MediaPlayer_prepareAsync_search_buildable.json']

if __name__ == "__main__":

    path = '/home/ubuntu/data/search'

    config = { 'host':'13.58.20.201', 'port':6379, 'db':0, 'jobs':'build_jobs', 'name':'phoenix', 'pwd':'phoenix-has-died', 'done':'done_jobs', 'failed':'failed_jobs' }
    
    for queue,json in queues.items():
       load_search_data(queue, json_file = "%s/%s" % (path,json), redis_store=None, config=config, unique_repo=True)
