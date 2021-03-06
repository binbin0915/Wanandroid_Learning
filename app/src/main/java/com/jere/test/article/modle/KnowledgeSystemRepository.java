package com.jere.test.article.modle;

import com.google.gson.Gson;
import com.jere.test.article.modle.api.AbstractRetrofitCallback;
import com.jere.test.article.modle.api.ApiService;
import com.jere.test.article.modle.api.ApiWrapper;
import com.jere.test.article.modle.api.GetWebDataListener;
import com.jere.test.article.modle.beanfiles.knowledgesystem.KnowledgeSystemArticleListBean;
import com.jere.test.article.modle.beanfiles.knowledgesystem.KnowledgeSystemCategoryBean;

/**
 * @author jere
 */
public class KnowledgeSystemRepository {

    /**
     * 使用静态内部类来创建单例
     */
    private static class KnowledgeSystemRepositoryHelper {
        private static KnowledgeSystemRepository instance = new KnowledgeSystemRepository();
    }

    public static KnowledgeSystemRepository getInstance() {
        return KnowledgeSystemRepositoryHelper.instance;
    }

    public void getKnowledgeSystemData(final GetWebDataListener listener) {
        ApiService apiService = ApiWrapper.getRetrofitInstance().create(ApiService.class);
        apiService.getKnowledgeSystem().enqueue(new AbstractRetrofitCallback() {
            @Override
            public void getSuccessful(String responseBody) {
                Gson gson = new Gson();
                KnowledgeSystemCategoryBean knowledgeSystemCategoryBean = gson.fromJson(responseBody, KnowledgeSystemCategoryBean.class);
                listener.getDataSuccess(knowledgeSystemCategoryBean);
            }

            @Override
            public void getFailed(String failedMsg) {
                listener.getDataFailed(failedMsg);
            }
        });
    }

    public void getKnowledgeSystemArticleList(int cid, final GetWebDataListener listener) {
        ApiService apiService = ApiWrapper.getRetrofitInstance().create(ApiService.class);
        apiService.getKnowledgeSystemArticleList(cid).enqueue(new AbstractRetrofitCallback() {
            @Override
            public void getSuccessful(String responseBody) {
                Gson gson = new Gson();
                KnowledgeSystemArticleListBean knowledgeSystemArticleListBean =
                        gson.fromJson(responseBody, KnowledgeSystemArticleListBean.class);
                listener.getDataSuccess(knowledgeSystemArticleListBean);
            }

            @Override
            public void getFailed(String failedMsg) {
                listener.getDataFailed(failedMsg);
            }
        });
    }

}
