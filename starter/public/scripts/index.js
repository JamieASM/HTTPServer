// On website load, do the following
document.addEventListener("DOMContentLoaded", function () {
    // ----------------------------------------------------------------------
    // Set up HTML Elements

    // key: queue id, value: concert id
    const queueMap = new Map();
    // key: queue id, value: artist
    const artistMap = new Map();
    // key: ticketID, value {artist, concert id}
    const purchasedTickets = new Map();

    // Get ticket elements
    const ticketsSection = document.getElementById("ticket-section");
    const ticketElement = document.querySelector(".ticket-information");

    // Get Queue elements
    const queueSection = document.getElementById("queue-info");
    const queueElement = document.querySelector(".queue-element");
    const queueElementClone = queueElement.cloneNode(true);

    // Get the My Purchased Ticket elements
    const purchasedSection = document.getElementById("refund");
    const purchasedElement = document.querySelector(".purchased-ticket");
    const purchasedElementClone = purchasedElement.cloneNode(true);

    // remove the original ticket and queue element.
    ticketElement.style.display = "none";
    queueElement.remove();

    // Grab the selector element and attach an event listener
    const dropdown = document.getElementById("selector");

    dropdown.addEventListener("change", event => {
        const value = event.target.value;

        if (value === "-1") {
            ticketElement.style.display = "none";
        } else {
            requestInfo(value);
        }
    })

    /**
     * Adds a new artist's concert to the dropdown.
     * @param concert
     */
    function appendToDropdown(concert) {
        const option = document.createElement("option");

        option.value = concert.id;
        option.text = `${concert.artist}: ${makeHumanReadable(concert.dateTime)}`;

        dropdown.add(option, null);
    }

    // Populate the dropdown with all the artists.
    fetch("/tickets", {
        method: "GET",
        headers: {"Accept": "application/json"}
    })
        .then((response) => {
            // handle the case that the response is an error.
            if (!response.ok) {
                // only a 500 error is possible for this request type
                if (response.status === 500) {
                    return response.text().then(text => {
                        throw new Error(text || "Server Error");
                    })
                }
                throw new Error(response.statusText);
            }

            // otherwise, the response was ok
            return response.json();
        })
        .then(data => {
            data.concerts.forEach(
                concert => {
                    appendToDropdown(concert);
                }
            )
        })
        .catch(err => {
            addTicketError(err.message || "Failed to retrieve resources");
        });

    // ----------------------------------------------------------------------
    // GET specific ticket info:

    /**
     * Sends a 'GET ticket/{id}' request to the server.
     * @param id The unique id of the artist.
     */
    function requestInfo(id) {
        // Info about the tickets
        fetch(`/tickets/${id}`, {
            method: "GET",
            headers: {"Accept": "application/json"}
        })
            .then((response) => {
                // handle the case that the response is an error.
                if (!response.ok) {
                    // only a 500 error is possible for this request type
                    if (response.status === 500) {
                        return response.text().then(text => {
                            throw new Error(text || "Server Error");
                        })
                    }
                    throw new Error(response.statusText);
                }

                // otherwise, the response was ok
                return response.json();
            })
            .then(concert => {
                addConcert(concert)
            })
    }

    /**
     * Generates a p element to show the client the error.
     * @param message The error message.
     */
    function addTicketError(message) {
        const errorElement = document.createElement("p");
        errorElement.textContent = message;

        ticketsSection.appendChild(errorElement);
    }

    /**
     * Creates a HTML element to represent a concert instance.
     * @param json The JSON object that contains the concert data.
     */
    function addConcert(json) {
        ticketElement.style.display = "";

        // add the text
        ticketElement.getElementsByClassName("artist")[0].textContent = json.artist;
        ticketElement.getElementsByClassName("venue")[0].textContent = json.venue;
        ticketElement.getElementsByClassName("datetime")[0].textContent = makeHumanReadable(json.dateTime);
        ticketElement.getElementsByClassName("count")[0].textContent = json.count;

        // Add the concert to the button and an associated queue event listener
        const button = ticketElement.getElementsByClassName("join-queue")[0];
        button.dataset.concertID = json.id;

        // event listener
        button.addEventListener("click", () => {
            let numberOfTickets = requestNumberOfTickets();
            if (numberOfTickets !== null) {
                queue(button.dataset.concertID, numberOfTickets);
            }
        })
    }

    /**
     * Reformats the data to be human-readable.
     * @param datetime The date as represented by a ISO 8601 UTC string
     * @returns {string} The reformatted date. Uses dd/mm/yyyy.
     */
    function makeHumanReadable(datetime) {
        let day = datetime.slice(8,10);
        let month = datetime.slice(5,7);
        let year = datetime.slice(0, 4);

        return day + "/" + month + "/" + year;
    }

    // ----------------------------------------------------------------------
    // Making 'queue' requests

    /**
     * Requests the server to queue a ticket purchase request.
     * @param id The unique id of the concert
     * @param numberOfTickets The number of tickets the client wants to purchase.
     */
    function queue(id, numberOfTickets) {
        fetch(`/queue/${id}`, {
            method: "POST",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ tickets: numberOfTickets })
        })
            .then(res => {
                // 500 or 400 errors
                if (!res.ok) {
                    if (res.status === 400 || res.status === 500 ) {
                        // alert the user
                        return res.text().then(text => {
                            throw new Error(res.text || `Server error: ${res.status}`);
                        })
                    }
                    throw new Error(res.statusText);
                }

                return res.json()
            })
            .then(data => {
                window.alert(`Your request has been successfully added to the queue.\nYour Queue ID is: ${data.id}`);
                const artistName = ticketElement.querySelector(".artist").textContent;
                artistMap.set(data.id, artistName);
                queueMap.set(data.id, id);
            })
            .catch(error => {
                window.alert("Error: ", error);
            });
    }

    /**
     * Requests the user for the number of tickets they want to purchase.
     * @returns {number|null} The number of tickets the client wants to purchase.
     */
    function requestNumberOfTickets() {
        let res

        // prompt the user until the response is valid
        while(true) {
            res = prompt("Please enter the number of tickets to be purchased: ");

            // if the user cancels, return nothing.
            if (res === null) {
                return null;
            }

            res = res.trim();

            if (res !== "") {
                const number = Number(res);

                if(Number.isInteger(number) && number > 0) {
                    return number;
                }
            }
        }
    }

    // periodic call to the reload the queue
    let intervalID = setInterval(updateQueue, 2000);

    /**
     * Updates the html elements in the queue section
     */
    function updateQueue() {
        if (queueMap.size === 0) {
            return;
        }

        // grabs the data for each queue element
        const fetchData = Array.from(queueMap.entries()).map(([queueId, concertID]) => {
            return fetch(`/queue/${concertID}/${queueId}`, {
                method: "GET",
                headers: { "Accept": "application/json" }
            })
                .then(res => {
                    if (!res.ok) {
                        if (res.status === 404) {
                            return res.text().then(text => ({
                                position: -2,
                                errorMessage: text || "Queue not found",
                            }));
                        }
                        else if (res.status === 500) {
                            return res.text().then(text => ({
                                position: -2,
                                errorMessage: text || "Server Error",
                            }));
                        }
                    }

                    return res.json();
                })
                .then(data => {
                    // If purchase completed, update ticket count
                    if (data.position === -1 ) {
                        if (queueMap.has(data.id)) {
                            const artistName = artistMap.get(data.id);
                            purchasedTickets.set(data.ticketIds, {
                                artist: artistName,
                                concertId: concertID
                            })

                            displayPurchasedTickets(data.ticketIds);

                            queueMap.delete(data.id);
                            artistMap.delete(data.id);

                            // use the ticket information
                            updateTicketCount(concertID, -data.tickets);
                        }
                        return null; // and display nothing
                    }
                    return data;
                })
                .catch(error => {
                    return {
                        position: -2,
                        errorMessage: error.message || "Failed to retrieve queue information"
                    };
                });
        });

        Promise.all(fetchData).then(res => {
            document.querySelectorAll('.queue-element').forEach(e => e.remove());

            // Sort errors and valid results
            const validResults = res.filter(data => data && data.position >= 0);
            const errors = res.filter(data => data && data.position === -2);

            validResults.sort((a, b) => a.position - b.position);

            // display errors:
            if (errors.length > 0) {
                errors.forEach(error => {
                    addQueueError(error.errorMessage || "Failed to retrieve resources");
                })
            }

            // Add sorted queue elements
            validResults.forEach(data => {
                addQueueElement(data);
            });

        })
    }

    function addQueueError(message) {
        const errorElement = document.createElement("p");
        errorElement.textContent = message;
        errorElement.className = "queue-element";

        queueSection.appendChild(errorElement);
    }

    function updateTicketCount(concertID, numberOfTickets) {
        if (ticketElement.style.display !== "none") {
            const button = ticketElement.getElementsByClassName("join-queue")[0];
            if (button.dataset.concertID === concertID) {
                const count = ticketElement.querySelector(".count");
                const currentCount = parseInt(count.textContent);
                count.textContent = currentCount + numberOfTickets;
            }
        }
    }

    function addQueueElement(data) {
        const clone = queueElementClone.cloneNode(true);
        clone.style.display = "";

        clone.getElementsByClassName("artist")[0].textContent = artistMap.get(data.id);
        clone.getElementsByClassName("position")[0].textContent = data.position;

        const button = clone.getElementsByClassName("cancel")[0];
        button.dataset.queueID = data.id;

        // event listener
        button.addEventListener("click", () => {
            fetch(`/queue/${button.dataset.queueID}`, {
                method: "DELETE",
                headers: { "Accept": "application/json" }
            })
                .then(res => {
                    if (res.ok) {
                        queueMap.delete(button.dataset.queueID);
                        artistMap.delete(button.dataset.queueID);

                        clone.remove();
                    } else if (res.status === 404) {
                        alert("Queue item not found on server.");
                    } else if (res.status === 500) {
                        alert("Server error. Try again later.");
                    } else {
                        alert(`Error ${res.statusText}`);
                    }
                })
                .catch(error => {
                    alert("Failed to cancel queue: " + error.message)
                });
        });

        queueSection.appendChild(clone);
    }

    // ------------------------------------------------------------------------------------------
    // Refunds

    function displayPurchasedTickets(ticketIds) {
        purchasedSection.querySelectorAll('.purchased-ticket').forEach(e => e.remove());

        const clone = purchasedElementClone.cloneNode(true);
        clone.style.display = "";

        const ticketInfo = purchasedTickets.get(ticketIds);
        if (!ticketInfo) {
            return
        }

        clone.getElementsByClassName("artist")[0].textContent = ticketInfo.artist;
        clone.getElementsByClassName("ticket-id")[0].textContent = ticketIds;

        const button = clone.getElementsByClassName("refund")[0];

        button.addEventListener("click", () => {
            requestRefund(ticketIds);
        });

        purchasedSection.appendChild(clone);
    }

    function requestRefund(ticketIDs) {
        if (!confirm("Are you sure you want to cancel this purchase?")) return;

        fetch(`/tickets/refund`, {
            method: "POST",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ ticketIDs }) // fixed comma
        })
            .then(res => {
                if (!res.ok) throw new Error(`Server error: ${res.status}`);
                return res.json();
            })
            .then(updatedData => {
                const ticketInfo = purchasedTickets.get(ticketIDs);

                if (ticketInfo && updatedData.refundedCount) {
                    updateTicketCount(ticketInfo.concertId, updatedData.refundedCount);
                }

                purchasedTickets.delete(ticketIDs);
                displayPurchasedTickets(null);
                alert("Tickets refunded successfully")
            })
            .catch(err => {
                alert("Failed to refund ticket: " + err.message);
            });
    }

})

