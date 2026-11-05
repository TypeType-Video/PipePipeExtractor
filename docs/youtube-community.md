# YouTube community posts and comment attachments

This API is read-only. It exposes YouTube's channel posts and comment attachments;
it does not create posts, vote in polls, or send comments.

## Channel posts

The YouTube channel extractor can expose a `ChannelTabs.POSTS` tab. A caller can
also request it directly with a channel's `/posts` URL (`/community` is accepted
as an alias):

```java
ListLinkHandler tab = service.getChannelTabLHFactory()
        .fromUrl("https://www.youtube.com/@example/posts");
ChannelTabInfo page = ChannelTabInfo.getInfo(service, tab);
for (InfoItem item : page.getRelatedItems()) {
    if (item instanceof CommunityPostInfoItem) {
        CommunityPostInfoItem post = (CommunityPostInfoItem) item;
        // Render post.getContent(), post.getAuthorName(), post.getPostType(), etc.
    }
}
// If page.getNextPage() is not null, use ChannelTabInfo.getMoreItems(service, tab, page.getNextPage()).
```

`CommunityPostInfoItem.getImages()` groups image variants by attachment. Poll
choices have their own optional images; shared videos have a URL, title and
preview variants. For a repost, `getSharedPost()` contains the original post if
YouTube returned it. A parsed date can be unavailable even when the textual
date is present. Counts are the localized strings returned by YouTube, not
necessarily exact numbers. Unknown or incomplete attachment variants should
not be interpreted as proof that a post has no media.

Adding the extractor API does not add a community-post endpoint to TypeType by
itself: TypeType Server still needs to map `ChannelTabs.POSTS` and
`InfoType.COMMUNITY_POST` into its public API, then its clients can render the
new item type.

## Images and GIFs in comments

`CommentsInfoItem.getPictures()` provides available image URL variants.
`getPictureAnimation()` is `ANIMATED` when the service identifies the
attachment as animated, otherwise `UNKNOWN`; `getPictureFormatHint()` retains
the service's format hint when one is available. For example, YouTube may use an
animated WebP URL for a GIF. A missing hint does not prove an image is static.
Keep the full URL including its query string, which may be necessary to fetch
the media.

TypeType Server still needs to expose these attachment fields in its comment
response. The consuming client also needs an image decoder and view that support
animated WebP or the returned format. Extracting the URL alone does not guarantee
animated display.
