package com.leeeyou.wanandroid

import `in`.srain.cube.views.ptr.PtrFrameLayout
import `in`.srain.cube.views.ptr.PtrHandler
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.leeeyou.R
import com.leeeyou.manager.BaseFragment
import com.leeeyou.util.inflate
import com.leeeyou.util.startBrowserActivity
import com.leeeyou.wanandroid.model.bean.Banner
import com.leeeyou.wanandroid.model.bean.RecommendItem
import com.leeeyou.wanandroid.model.bean.RecommendList
import com.leeeyou.wanandroid.model.bean.ResponseBanner
import com.leeeyou.wanandroid.model.fetchBannerList
import com.leeeyou.wanandroid.model.fetchRecommendList
import com.youth.banner.Transformer
import com.youth.banner.loader.ImageLoader
import kotlinx.android.synthetic.main.fragment_wan_android_recommend.*
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber


/**
 * ClassName:   WeatherFragment
 * Description: Play Android - Recommend
 *
 * Author:      leeeyou
 * Date:        2017/4/24 13:46
 */
class WanAndroidRecommendFragment : BaseFragment() {
    lateinit var mLinearLayoutManager: LinearLayoutManager
    var mPageIndex: Int = 0
    private lateinit var mRecommendAdapter: BaseQuickAdapter<RecommendItem, BaseViewHolder>
    private var mPageCount: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container?.inflate(R.layout.fragment_wan_android_recommend)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
        fetchBannerListFromServer()
        fetchRecommendListFromServer(mPageIndex)
    }

    private fun initView() {
        initBanner()
        initPtrFrame()
        initRecyclerView()
    }

    private fun initRecyclerView() {
        mLinearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mRecommendAdapter = object : BaseQuickAdapter<RecommendItem, BaseViewHolder>(R.layout.item_recommend, null) {
            override fun convert(helper: BaseViewHolder?, item: RecommendItem?) {
                item?.takeIf { it.visible == 1 }?.also {
                    helper?.setText(R.id.tv_title, it.title)
                            ?.setText(R.id.tv_author, "作者:" + it.author)
                            ?.setText(R.id.tv_category, "分类:" + it.superChapterName + " / " + it.chapterName)
                            ?.setText(R.id.tv_niceDate, it.niceDate)
                            ?.setGone(R.id.tv_refresh, it.fresh)
                }
            }
        }
        mRecommendAdapter.setOnLoadMoreListener({
            if (mPageIndex + 1 == mPageCount) {
                mRecommendAdapter.loadMoreEnd()
            } else {
                fetchRecommendListFromServer(++mPageIndex)
            }
        }, recyclerViewRecommend)
        mRecommendAdapter.setOnItemClickListener { adapter, _, position ->
            val item: RecommendItem = adapter.getItem(position) as RecommendItem
            startBrowserActivity(context!!, item.link, item.title)
        }
        mRecommendAdapter.openLoadAnimation(BaseQuickAdapter.SCALEIN)

        recyclerViewRecommend.layoutManager = mLinearLayoutManager
        recyclerViewRecommend.adapter = mRecommendAdapter
    }

    private fun initBanner() {
        banner.setImageLoader(object : ImageLoader() {
            override fun displayImage(context: Context?, path: Any?, imageView: ImageView?) {
                context?.let {
                    imageView?.let {
                        Glide.with(context).load(path).into(imageView)
                    }
                }
            }
        })
        banner.setDelayTime(3000)
    }

    private fun initPtrFrame() {
        ptrFrameRecommend.disableWhenHorizontalMove(true)
        ptrFrameRecommend.setPtrHandler(object : PtrHandler {
            override fun onRefreshBegin(frame: PtrFrameLayout?) {
                fetchBannerListFromServer()

                mPageIndex = 0
                fetchRecommendListFromServer(mPageIndex)
            }

            override fun checkCanDoRefresh(frame: PtrFrameLayout?, content: View?, header: View?): Boolean {
                return mLinearLayoutManager.findFirstCompletelyVisibleItemPosition() <= 0
            }
        })
    }

    private fun fetchRecommendListFromServer(pageIndex: Int) {
        fetchRecommendList(pageIndex)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    Timber.d("fetchRecommendListFromServer doOnNext")

                    ptrFrameRecommend.refreshComplete()

                    it.takeIf { response ->
                        response.errorCode >= 0
                    }?.also { response ->
                        Timber.d(response.data.toString())
                        renderRecommendList(pageIndex, response.data)
                    } ?: IllegalArgumentException("fetchRecommendListFromServer接口返回异常")
                }
                .doOnError {
                    ptrFrameRecommend.refreshComplete()
                    Timber.e(it, "fetchRecommendListFromServer doOnError")
                }
                .doOnCompleted {
                    Timber.d("fetchRecommendListFromServer doOnCompleted")
                    if (mPageIndex > 0) {
                        mRecommendAdapter.loadMoreComplete()
                    }

                }
                .subscribe()
    }

    private fun renderRecommendList(witchPage: Int, data: RecommendList) {
        if (witchPage == 0) {
            mRecommendAdapter.setNewData(data.datas)
        } else {
            mPageCount = data.pageCount
            mRecommendAdapter.addData(data.datas)
        }
    }

    private fun fetchBannerListFromServer() {
        fetchBannerList()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Subscriber<ResponseBanner>() {
                    override fun onNext(responseBanner: ResponseBanner) {
                        Timber.d("fetchBannerList onNext , result is %s ", responseBanner.toString())

                        responseBanner.takeIf {
                            it.errorCode >= 0
                        }?.also {
                            renderBanner(it.data)
                        } ?: onError(IllegalArgumentException("Banner接口返回异常"))
                    }

                    override fun onCompleted() {
                        Timber.d("fetchBannerList onCompleted")
                    }

                    override fun onError(e: Throwable?) {
                        Timber.d(e, "fetchBannerList onError")
                    }
                })
    }

    private fun renderBanner(bannerList: List<Banner>) {
        bannerList.map { it.imagePath }.run {
            banner.setImages(this)
            banner.setBannerAnimation(Transformer.Default)
            banner.setOnBannerListener { it ->
                val banner = bannerList[it]
                Timber.d("click banner position is %s , the url is %s", it, banner.url)
                startBrowserActivity(context!!, banner.url, banner.title)
            }
            banner.start()
        }
    }

    override fun onStart() {
        super.onStart()
        banner.startAutoPlay()
    }

    override fun onStop() {
        super.onStop()
        banner.stopAutoPlay()
    }

}