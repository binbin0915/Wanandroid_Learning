package com.jere.test.article.view.homearticle;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.jere.test.R;
import com.jere.test.article.modle.beanfiles.homearticle.HomeArticleListBean;
import com.jere.test.article.modle.beanfiles.homearticle.HomeBannerListBean;
import com.jere.test.article.view.ArticleDetailWebViewActivity;
import com.jere.test.article.viewmodel.homearticle.HomeViewModel;
import com.jere.test.databinding.FragmentHomeBinding;
import com.jere.test.util.RecyclerItemClickListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

/**
 * @author jere
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private OnFragmentInteractionListener mListener;
    private ViewPager2 mBannerVp2;
    private MyBannerVpAdapter mBannerVpAdapter;
    private View[] indicateViews;
    private ArrayList<HomeBannerListBean.DataBean> mBannerDataList = new ArrayList<>();
    private BannerHandler mBannerHandler;
    private ScheduledExecutorService mBannerScheduledExecutorService;
    private HomeArticleListViewAdapter mArticleListViewAdapter;
    private ArrayList<HomeArticleListBean.DataBean.DatasBean> mHomeArticleListData = new ArrayList<>();
    private FragmentHomeBinding mBinding;

    private Observer<HomeBannerListBean> bannerListDataObserver = new Observer<HomeBannerListBean>() {
        @Override
        public void onChanged(HomeBannerListBean homeBannerListBean) {
            if (homeBannerListBean != null) {
                List<HomeBannerListBean.DataBean> bannerListDatas = homeBannerListBean.getData();
                mBannerDataList = new ArrayList<>();
                HomeBannerListBean.DataBean fakeDataBean = new HomeBannerListBean.DataBean();
                //为了实现Banner循环轮播，需要额外多两张图片，分别放置列表首尾。
                mBannerDataList.add(fakeDataBean);
                mBannerDataList.addAll(bannerListDatas);
                mBannerDataList.add(fakeDataBean);
                mBannerVpAdapter = new MyBannerVpAdapter(HomeFragment.this, mBannerDataList);
                mBannerVpAdapter.notifyDataSetChanged();
                mBannerVp2.setAdapter(mBannerVpAdapter);
                mBannerVp2.setCurrentItem(1);
            }
        }
    };

    private Observer<HomeArticleListBean> articleListBeanObserver = new Observer<HomeArticleListBean>() {
        @Override
        public void onChanged(HomeArticleListBean homeArticleListBean) {
            if (homeArticleListBean != null) {
                mHomeArticleListData = homeArticleListBean.getData().getDatas();
                mArticleListViewAdapter = new HomeArticleListViewAdapter(mHomeArticleListData);
                mBinding.homeArticleListRecycleView.setAdapter(mArticleListViewAdapter);
            }
        }
    };

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mBinding = FragmentHomeBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.e(TAG, "onViewCreated: ");
        HomeViewModel homeVm = ViewModelProviders.of(this, new ViewModelFactory()).get(HomeViewModel.class);
        homeVm.getHomeBannerListLd().observe(getViewLifecycleOwner(), bannerListDataObserver);
        homeVm.setHomeBannerListLd();
        homeVm.getHomeArticleListBeanLd().observe(getViewLifecycleOwner(), articleListBeanObserver);
        homeVm.setHomeArticleListBeanLd();

        initBannerVew();
        mBannerHandler = new BannerHandler(this);
        startAutoLoopBanner();

        initArticleListView();
    }

    private void initBannerVew() {
        mBannerVp2 = mBinding.homeBannerVp2;
        indicateViews = new View[]{
                mBinding.firstIndicateView,
                mBinding.secondIndicateView,
                mBinding.thirdIndicateView,
                mBinding.fourthIndicateView};
        mBannerVpAdapter = new MyBannerVpAdapter(this, mBannerDataList);
        mBannerVp2.setAdapter(mBannerVpAdapter);
        mBannerVp2.setCurrentItem(1);
        mBannerVp2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                //hard code, Banner只有4张图
                if (position == mBannerDataList.size() - 1) {
                    mBannerVp2.setCurrentItem(1);
                    position = 1;
                } else if (position == 0) {
                    mBannerVp2.setCurrentItem(4);
                    position = 4;
                }
                for (int i = 1; i < 5; i++) {
                    if (position == i) {
                        indicateViews[i - 1].setBackgroundResource(R.drawable.banner_navigation_point_highlight_shape);
                    } else {
                        indicateViews[i - 1].setBackgroundResource(R.drawable.banner_navigation_point_gray_shape);
                    }
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    mBannerScheduledExecutorService.shutdownNow();
                }
            }
        });
    }

    private void startAutoLoopBanner() {
        mBannerScheduledExecutorService = Executors.newScheduledThreadPool(1);
        mBannerScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = 1;
                mBannerHandler.sendMessage(msg);
            }
        }, 2, 3, TimeUnit.SECONDS);
    }


    private void initArticleListView() {
        mArticleListViewAdapter = new HomeArticleListViewAdapter(mHomeArticleListData);
        mBinding.homeArticleListRecycleView.setAdapter(mArticleListViewAdapter);
        mBinding.homeArticleListRecycleView.addOnItemTouchListener(new RecyclerItemClickListener(getContext(),
                mBinding.homeArticleListRecycleView,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        String link = mHomeArticleListData.get(position).getLink();
                        Intent intent = new Intent(getActivity(), ArticleDetailWebViewActivity.class);
                        intent.putExtra(ArticleDetailWebViewActivity.ARTICLE_DETAIL_WEB_LINK_KEY, link);
                        startActivity(intent);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }
                }));

    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: mBannerScheduledExecutorService.isShutdown() = " + mBannerScheduledExecutorService.isShutdown());
        if (!mBannerScheduledExecutorService.isShutdown()) {
            mBannerScheduledExecutorService.shutdown();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public static class BannerHandler extends Handler {
        private WeakReference<HomeFragment> weakReference;

        BannerHandler(HomeFragment homeFragment) {
            weakReference = new WeakReference<>(homeFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            HomeFragment homeFragment = weakReference.get();
            if (msg.what == 1) {
                if (homeFragment != null && !homeFragment.isHidden()) {
                    int currentItem = homeFragment.mBannerVp2.getCurrentItem();
                    if (currentItem == 4) {
                        homeFragment.mBannerVp2.setCurrentItem(1);
                    } else {
                        homeFragment.mBannerVp2.setCurrentItem(currentItem + 1);
                    }
                }
            }
        }
    }

    class MyBannerVpAdapter extends RecyclerView.Adapter<MyBannerVpAdapter.MyViewHolder> {
        private ArrayList<HomeBannerListBean.DataBean> bannerDataList;
        private WeakReference<HomeFragment> weakReference;

        MyBannerVpAdapter(HomeFragment fragment, ArrayList<HomeBannerListBean.DataBean> dataList) {
            this.weakReference = new WeakReference<>(fragment);
            this.bannerDataList = dataList;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.banner_view_page_item, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            HomeBannerListBean.DataBean itemData = bannerDataList.get(position);
            String imageUrl = itemData.getImagePath();
            final HomeFragment homeFragment = weakReference.get();
            if (!TextUtils.isEmpty(imageUrl) && homeFragment != null
                    && !homeFragment.isHidden()
                    && !TextUtils.isEmpty(imageUrl)) {
                Glide.with(homeFragment).load(imageUrl).into(holder.bannerItemIv);
            }

            final String articleDetailUrl = itemData.getUrl();
            holder.bannerItemIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "onClick: bannerItemIv");
                    Intent intent = new Intent(homeFragment.getActivity(), ArticleDetailWebViewActivity.class);
                    intent.putExtra(ArticleDetailWebViewActivity.ARTICLE_DETAIL_WEB_LINK_KEY, articleDetailUrl);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return bannerDataList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            private ImageView bannerItemIv;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                bannerItemIv = itemView.findViewById(R.id.banner_item_iv);
            }
        }
    }

    class ViewModelFactory implements ViewModelProvider.Factory {

        @Override
        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(HomeViewModel.class)) {
                return (T) new HomeViewModel();
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }

    class HomeArticleListViewAdapter extends RecyclerView.Adapter<HomeArticleListViewAdapter.MyViewHolder> {
        private List<HomeArticleListBean.DataBean.DatasBean> homeArticleListData;

        HomeArticleListViewAdapter(List<HomeArticleListBean.DataBean.DatasBean> homeArticleListData) {
            this.homeArticleListData = homeArticleListData;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycler_item_view_article_list_item, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            HomeArticleListBean.DataBean.DatasBean data = homeArticleListData.get(position);
            holder.titleTv.setText(data.getTitle());
            String author;
            if (!TextUtils.isEmpty(data.getAuthor())) {
                author = data.getAuthor();
            } else if (TextUtils.isEmpty(data.getShareUser())) {
                author = data.getShareUser();
            } else {
                author = "Robot";
            }
            holder.authorTv.setText(author);
            holder.sharedDateTv.setText(data.getNiceShareDate());
        }

        @Override
        public int getItemCount() {
            return homeArticleListData.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            private TextView titleTv;
            private TextView authorTv;
            private TextView sharedDateTv;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTv = itemView.findViewById(R.id.articleListItemTitleTv);
                authorTv = itemView.findViewById(R.id.articleListItemAuthorTv);
                sharedDateTv = itemView.findViewById(R.id.articleListItemSharedDateTv);
            }
        }
    }
}
