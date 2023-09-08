$(document).ready(function () {
  $("#confirmationModal").on("show.bs.modal", function (event) {
    var button = $(event.relatedTarget); // Button that triggered the modal
    var action = button.data("action"); // Action from data-action attribute
    var modal = $(this);

    // Set the modal title and body content based on the action
    if (action === "delete") {
      modal.find(".modal-title").text("Delete Confirmation");
      modal
        .find(".modal-body")
        .text("Are you sure you want to delete this item?");
      modal.find("#confirmationModalAction").text("Delete");
    } else if (action === "update") {
      modal.find(".modal-title").text("Update Confirmation");
      modal
        .find(".modal-body")
        .text("Are you sure you want to update this item?");
      modal.find("#confirmationModalAction").text("Update");
    }

    // Handle the confirmation action when the modal's action button is clicked
    modal
      .find("#confirmationModalAction")
      .off("click")
      .on("click", function () {
        if (action === "delete") {
          // Redirect to the delete URL
          window.location.href = button.attr("href");
        } else if (action === "update") {
          // Submit the form
          button.closest("form").submit();
        }
      });
  });
});
