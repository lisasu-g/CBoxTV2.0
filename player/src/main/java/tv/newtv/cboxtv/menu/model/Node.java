package tv.newtv.cboxtv.menu.model;

import android.text.TextUtils;

import com.newtv.cms.bean.Content;
import com.newtv.libs.Constant;

import java.util.ArrayList;
import java.util.List;

import tv.newtv.cboxtv.menu.LastMenuRecyclerAdapter;
import tv.newtv.cboxtv.menu.MenuGroupPresenter2;

/**
 * Created by TCP on 2018/4/18.
 */

public class Node {
    private String id;
    private String title;
    private String pid;
    private String img;
    /**
     * 类似于这样OPEN_TV OPEN_PS
     */
    private String actionType;
    private String actionUri;
    /**
     * 类似于这样TV PS
     */
    private String contentType;

    private String categoryType;

    private Node parent;
    protected List<Node> child = new ArrayList<>();
    private List<Program> programs = new ArrayList<>();
    private LastMenuBean lastMenuBean;
    private Content content;
    /**
     * 是否已经请求完数据了
     */
    private boolean request = false;
    /**
     * 是否禁止添加LB频道的收藏
     */
    private boolean forbidAddCollect = false;
    /**
     * 是否必须请求数据
     */
    private boolean mustRequest = false;
    /**
     * 是否正在请求数据
     */
    private boolean requesting = false;

    public boolean isLeaf(){
//        if(child != null && child.size() > 0)
            return false;
//        return true;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isRequest() {
        return request;
    }

    public void setRequest(boolean request) {
        this.request = request;
    }

    public LastMenuBean getLastMenuBean() {
        return lastMenuBean;
    }

    public void setLastMenuBean(LastMenuBean lastMenuBean) {
        this.lastMenuBean = lastMenuBean;
    }

    public int getLevel(){
        return parent == null ? 0 : parent.getLevel() + 1;
    }

    public List<Program> getPrograms() {
        if(Constant.CONTENTTYPE_LV.equals(contentType) && programs.size() == 0){
            addNoContents(programs);
        }
        return programs;
    }

    public void setPrograms(List<Program> programs) {
        this.programs = programs;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public List<Node> getChild() {
        if(child == null){
            child = new ArrayList<>();
        }
        return child;
    }

    public void setChild(List<Node> child) {
        this.child = child;
    }

    public void addChild(List<LastNode> child){
        addChild(child,false);
    }

    public void addChild(List<LastNode> child,boolean reset){
        if(reset){
            this.child.clear();
        }
        for(LastNode lastNode : child){
            lastNode.setParent(this);
            this.child.add(lastNode);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionUri() {
        return actionUri;
    }

    public void setActionUri(String actionUri) {
        this.actionUri = actionUri;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public boolean isForbidAddCollect() {
        return forbidAddCollect;
    }

    public void setForbidAddCollect(boolean forbidAddCollect) {
        this.forbidAddCollect = forbidAddCollect;
    }

    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    public boolean isMustRequest() {
        return mustRequest;
    }

    public void setMustRequest(boolean mustRequest) {
        this.mustRequest = mustRequest;
    }

    public List<Node> getNodes(){
        List<Node> result = new ArrayList<>();
        result.add(this);
        for(Node node : child){
            result.addAll(node.getNodes());
        }
        return result;
    }

    public Node searchNode(String id){
        Node result = null;
        if(TextUtils.equals(getId(),id)){
            return this;
        }
        for(Node node : child){
            result = node.searchNode(id);
            if(result != null){
                return result;
            }
        }
        return null;
    }

    public Node searchNodeInParent(String id){
        Node result = null;
        if(TextUtils.equals(getId(),id)){
            return this;
        }
        if (getParent() != null){
            result = getParent().searchNodeInParent(id);
            if(result != null){
                return result;
            }
        }
        return null;
    }

    public boolean isLbCollectNodeOrChild(){
        boolean result;
        result = searchNodeInParent(MenuGroupPresenter2.LB_ID_COLLECT) != null ? true : false;
        return result;
    }

    public Node searchNodeInParentByCategory(String type){
        Node result = null;
        if(TextUtils.equals(categoryType,type)){
            return this;
        }
        if (getParent() != null){
            result = getParent().searchNodeInParentByCategory(type);
            if(result != null){
                return result;
            }
        }
        return null;
    }

    public boolean isLbNodeOrChild(){
        boolean result;
        result = searchNodeInParentByCategory(Constant.CONTENTTYPE_LB) != null ? true : false;
        return result;
    }

    public Node getLeftNode(){
        if(getParent() != null){
            int size = getParent().getChild().size();
            int number = getParent().getChild().indexOf(this);
            number--;
            if(number < 0) {
                number = size - 1;
            }
            return getParent().getChild().get(number);
        }
        return null;
    }

    public Node getRightNode(){
        if(getParent() != null){
            int size = getParent().getChild().size();
            int number = getParent().getChild().indexOf(this);
            number++;
            if(number >= size) {
                number = 0;
            }
            return getParent().getChild().get(number);
        }
        return null;
    }

    public void initParent(){
        for(Node node : child){
            node.initParent();
            node.setParent(this);
            node.setPid(getId());
        }
    }

    public boolean containId(String id){
        if(TextUtils.isEmpty(id)){
            return false;
        }
        if(TextUtils.equals(id,getId())){
            return true;
        }
        Node node = parent;
        while(node != null){
            if(TextUtils.equals(id,node.getId())){
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    private void addNoContents(List<Program> list){
        Program program = new Program();
        program.setTitle("暂无节目单");
        program.setContentUUID(LastMenuRecyclerAdapter.NO_CONTENTS);
        program.setParent(this);
        list.add(program);
    }

    public boolean isRequesting() {
        return requesting;
    }

    public void setRequesting(boolean requesting) {
        this.requesting = requesting;
    }
}
